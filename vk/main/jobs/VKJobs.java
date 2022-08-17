package com.aboba.vk.main.jobs;

import com.aboba.domain.s3.S3Service;
import com.aboba.domain.vk.main.entity.Friend;
import com.aboba.domain.vk.main.entity.ScheduledStories;
import com.aboba.domain.vk.main.entity.VkAccountSettings;
import com.aboba.domain.vk.main.repository.RepostedGroupRepository;
import com.aboba.domain.vk.main.repository.ScheduledStoriesRepository;
import com.aboba.domain.vk.main.repository.VkAccountRepository;
import com.aboba.domain.vk.metrics.VkMetricService;
import com.aboba.exception.CustomControllerAdvice;
import com.aboba.exception.MismatchEnumException;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ApiPermissionException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.friends.RequestsXtrMessage;
import com.vk.api.sdk.objects.likes.Type;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.wall.Wallpost;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;

@NoArgsConstructor
public class VKJobs {
    private static final int NUMBER_OF_POSTS = 5;
    private final Logger logger = LoggerFactory.getLogger(CustomControllerAdvice.class);

    @Autowired
    private S3Service s3Service;

    @Autowired
    private VkMetricService vkMetricService;

    @Autowired
    private VkAccountRepository vkRepository;

    @Autowired
    private RepostedGroupRepository repostedGroupRepository;

    @Autowired
    private ScheduledStoriesRepository scheduledStoriesRepository;


    @Transactional
    @Scheduled(cron = "@daily")
    public void checkNewPostsForRepostInGroups()
            throws ClientException, ApiException, InterruptedException, MismatchEnumException {
        logger.info("Checking new posts in groups started.");
        var credentials = vkRepository.findAllBySettingsRepostingNewPostsInGroupsTrue();
        var vk = new VkApiClient(new HttpTransportClient());
        int count = 0;
        for (var credential : credentials) {
            var settings = credential.getSettings();
            var actor = new UserActor(credential.getId(), credential.getAccessToken());
            var groups = settings.getRepostedGroups();

            int clientCount = 0;
            for (var group : groups) {
                if (group.isAutoReposted()) {
                    var postIds = getNewPostsIdsForLastDay(vk, actor, group.getGroupId());
                    for (var postId : postIds) {
                        if (clientCount > 50) {
                            clientCount = 0;
                            break;
                        }

                        if (count == 10) {
                            count = 0;
                            Thread.sleep(60000);
                        }
                        repostPost(vk, actor, postId, group.getGroupId());
                        count++;
                        clientCount++;
                    }
                }
            }
            Thread.sleep(60000);
        }
    }

    @Transactional
    @Scheduled(cron = "@daily")
    public void checkNewPostsForLikingInGroups()
            throws ClientException, ApiException, InterruptedException, MismatchEnumException {
        logger.info("Checking new posts in groups for liking.");
        var credentials = vkRepository.findAllBySettingsLikingNewPostsInGroupsTrue();
        var vk = new VkApiClient(new HttpTransportClient());

        for (var credential : credentials) {
            var settings = credential.getSettings();
            var actor = new UserActor(credential.getId(), credential.getAccessToken());
            var groups = settings.getSubscribedGroups();
            for (var group : groups) {
                var postIds = getNewPostsIdsForLastDay(vk, actor, group.getGroupId());
                for (var postId : postIds) {
                    likePost(vk, actor, postId, group.getGroupId());
                }
            }
        }
    }

    @Transactional
    @Scheduled(cron = "@daily")
    public void checkNewPostsForCommentingInGroups()
            throws ClientException, ApiException, MismatchEnumException, InterruptedException {
        logger.info("Checking new posts in groups for commenting.");
        var credentials = vkRepository.findAllBySettingsCommentingNewPostsInGroupsTrue();
        var vk = new VkApiClient(new HttpTransportClient());

        for (var credential : credentials) {
            var settings = credential.getSettings();
            var actor = new UserActor(credential.getId(), credential.getAccessToken());
            var groups = settings.getSubscribedGroups();
            var comments = settings.getGroupPostComments();
            if (comments.size() > 0) {
                var random = new Random();
                for (var group : groups) {
                    var postIds = getNewPostsIdsForLastDay(vk, actor, group.getGroupId());
                    for (var postId : postIds) {
                        var comment = comments.get(random.nextInt(comments.size())).getText();
                        createGroupPostComment(vk, actor, postId, group.getGroupId(), comment);
                    }
                }
            }
        }
    }

    private void createGroupPostComment(VkApiClient vk, UserActor actor, Integer postId, Integer groupId,
                                        String comment)
            throws ClientException, ApiException, InterruptedException, MismatchEnumException {
        vk.wall().createComment(actor, postId).ownerId(-groupId)
                .message(comment).execute();
        vkMetricService.createCommentMetric(actor.getId());
        Thread.sleep(200);
    }

    private void repostPost(VkApiClient vk, UserActor actor, Integer postId, Integer groupId)
            throws MismatchEnumException, InterruptedException, ClientException, ApiException {
        vk.wall().repost(actor, String.format("wall-%d_%d", groupId, postId)).execute();
        vkMetricService.createLikeMetric(actor.getId());
        Thread.sleep(500);
    }

    private void likePost(VkApiClient vk, UserActor actor, Integer postId, Integer groupId)
            throws MismatchEnumException, ClientException, ApiException, InterruptedException {
        vk.likes().add(actor, Type.POST, postId).ownerId(-groupId).execute();
        vkMetricService.createLikeMetric(actor.getId());
        Thread.sleep(500);
    }

    private long getNowMinusDay() {
        return LocalDateTime.now().minusDays(1).toEpochSecond(ZoneOffset.ofHours(3));
    }

    private List<Integer> getNewPostsIdsForLastDay(VkApiClient vk, UserActor actor, Integer groupId)
            throws ClientException, ApiException {
        var nowMinusDay = getNowMinusDay();
        return vk.wall().get(actor).ownerId(-groupId).count(NUMBER_OF_POSTS).execute()
                .getItems()
                .stream()
                .filter(item -> item.getDate() > nowMinusDay)
                .map(Wallpost::getId)
                .toList();
    }

    @Transactional
    @Scheduled(cron = "@daily")
    public void checkFriendsRequests()
            throws InterruptedException, ClientException, ApiException, MismatchEnumException {
        logger.info("Checking friends requests started.");
        var credentials = vkRepository.findAllBySettingsApprovingFriendshipTrue();
        var vk = new VkApiClient(new HttpTransportClient());
        for (var credential : credentials) {
            var settings = credential.getSettings();
            var vkId = credential.getId();
            var actor = new UserActor(vkId, credential.getAccessToken());

            var friendIds = getFriendshipRequests(vk, actor);
            for (var friendId : friendIds) {
                addNewFriend(vk, actor, friendId, settings);
                var liking = settings.isLikingPhotosWhenApproveFriendship();
                var commenting = settings.isCommentingPhotosWhenApproveFriendship();

                if (liking || commenting) {
                    var photoIds = getLastThreePhotosIds(vk, actor, friendId);
                    if (commenting) {
                        var random = new Random();
                        var comments = settings.getFriendPhotoComments();
                        if (comments.size() > 0) {
                            for (var photoId : photoIds) {
                                var randomComment = comments.get(random.nextInt(comments.size())).getText();
                                try {
                                    createFriendPhotoComment(vk, actor, friendId, photoId, randomComment);
                                } catch (ApiPermissionException e) {
                                    logger.info(String.format("User %s is not allowed to comment.", friendId));
                                    break;
                                }
                            }
                        }
                    }
                    if (settings.isLikingPhotosWhenApproveFriendship()) {
                        for (var photoId : photoIds) {
                            addLikeOnPhoto(vk, actor, friendId, photoId);
                        }
                    }
                }

                logger.info(String.format("User %s approve friendship request for %s", vkId,
                        friendIds));
            }
        }
    }

    private void addLikeOnPhoto(VkApiClient vk, UserActor actor, Integer friendId, Integer photoId)
            throws ClientException, ApiException, MismatchEnumException, InterruptedException {
        vk.likes().add(actor, Type.PHOTO, photoId).ownerId(friendId).execute(); //запрос
        vkMetricService.createLikeMetric(actor.getId());
        Thread.sleep(200);
    }

    private void createFriendPhotoComment(VkApiClient vk, UserActor actor, Integer friendId, Integer photoId,
                                          String randomComment)
            throws MismatchEnumException, ClientException, ApiException, InterruptedException {
        vk.photos().createComment(actor, photoId).ownerId(friendId)
                .message(randomComment)
                .execute(); //запрос
        vkMetricService.createCommentMetric(actor.getId());
        Thread.sleep(200);
    }

    private List<Integer> getLastThreePhotosIds(VkApiClient vk, UserActor actor, Integer friendId)
            throws ClientException, ApiException, InterruptedException {
        var photos =
                vk.photos().get(actor).ownerId(friendId).albumId("-6").count(3).rev(true).execute().getItems().stream()
                        .map(
                                Photo::getId).toList();
        Thread.sleep(200);

        return photos;
    }

    private void addNewFriend(VkApiClient vk, UserActor actor, Integer friendId, VkAccountSettings settings)
            throws ClientException, ApiException, MismatchEnumException, InterruptedException {
        vk.friends().add(actor).userId(friendId).execute(); //запрос
        settings.addFriend(new Friend(friendId));
        vkMetricService.createFriendMetric(actor.getId());
        Thread.sleep(200);
    }

    public List<Integer> getFriendshipRequests(VkApiClient vk, UserActor actor)
            throws InterruptedException, ClientException, ApiException {
        var friendIds = vk.friends().getRequestsExtended(actor).needViewed(true).execute().getItems().stream()
                .map(RequestsXtrMessage::getUserId).toList();
        Thread.sleep(200);

        return friendIds;
    }

    @Transactional
    @Scheduled(cron = "@daily")
    public void checkNewPhotos() throws ClientException, ApiException, MismatchEnumException, InterruptedException {
        logger.info("Checking new photos.");
        var credentials = vkRepository.findAllBySettingsLikingNewPhotosOfFriendsTrue();
        var vk = new VkApiClient(new HttpTransportClient());
        var nowMinusDay = getNowMinusDay();
        for (var credential : credentials) {
            var actor = new UserActor(credential.getId(), credential.getAccessToken());
            var friendIds = credential.getSettings().getFriends().stream().map(Friend::getVkId).toList();
            for (var friendId : friendIds) {
                var mainPhoto =
                        vk.photos().get(actor).ownerId(friendId).albumId("-6").count(1).rev(true).execute().getItems()
                                .get(0);
                if (mainPhoto.getDate() > nowMinusDay) {
                    addLikeOnPhoto(vk, actor, friendId, mainPhoto.getId());
                }
            }
        }
    }

    @Transactional
    @Scheduled(cron = "1 0 * * * *")
    public void checkScheduledStories()
            throws ClientException, ApiException, MismatchEnumException, InterruptedException {
        logger.info("Checking the scheduled stories started.");
        List<ScheduledStories> listOfScheduledStories =
                scheduledStoriesRepository.findAllByPublishDateBeforeOrderByPublishDateAsc(LocalDateTime.now());
        scheduledStoriesRepository.deleteAll(listOfScheduledStories);
        deleteStoriesFromS3(listOfScheduledStories);

        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        for (var stories : listOfScheduledStories) {
            var credentials = stories.getAccount();
            var actor = new UserActor(credentials.getId(), credentials.getAccessToken());

            publishStories(vk, actor, stories.getUploadResult());
        }
    }

    private void deleteStoriesFromS3(List<ScheduledStories> listOfScheduledStories) {
        for (var scheduledStories : listOfScheduledStories) {
            logger.info("Delete from s3 where key equals: " + scheduledStories.getS3ObjectKey());
            s3Service.deleteObject(scheduledStories.getS3ObjectKey());
        }
    }

    private void publishStories(VkApiClient vk, UserActor actor, String uploadResults)
            throws ClientException, ApiException, InterruptedException, MismatchEnumException {
        vk.stories().save(actor, uploadResults).execute();
        Thread.sleep(200);
        vkMetricService.createLikeMetric(actor.getId());
    }
}
