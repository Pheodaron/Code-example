package com.aboba.vk.main.repository;

import com.querydsl.core.BooleanBuilder;
import com.aboba.domain.s3.ImageService;
import com.aboba.domain.s3.S3Service;
import com.aboba.domain.s3.enums.EImage;
import com.aboba.domain.s3.enums.EVideo;
import com.aboba.domain.user.User;
import com.aboba.domain.user.service.KeyCloakService;
import com.aboba.domain.vk.main.dto.*;
import com.aboba.domain.vk.main.entity.*;
import com.aboba.domain.vk.main.entity.schedule.ScheduledPost;
import com.aboba.domain.vk.main.enums.ELastSeen;
import com.aboba.domain.vk.main.repository.*;
import com.aboba.domain.vk.main.threads.DownloadThread;
import com.aboba.domain.vk.metrics.VkMetricService;
import com.aboba.exception.EntityNotFoundException;
import com.aboba.exception.IO.WrongBodyStructureException;
import com.aboba.exception.IO.WrongContentTypeException;
import com.aboba.exception.MismatchEnumException;
import com.aboba.exception.security.NotAuthorizedException;
import com.aboba.exception.vk.VkNotEnoughRightsException;
import com.aboba.exception.vk.VkUserNotAuthorizedException;
import com.aboba.utils.DateTimeParseHelper;
import com.aboba.utils.dto.PaginateResponse;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.base.Country;
import com.vk.api.sdk.objects.database.City;
import com.vk.api.sdk.objects.groups.Fields;
import com.vk.api.sdk.objects.groups.UserXtrRole;
import com.vk.api.sdk.objects.groups.responses.GetByIdObjectLegacyResponse;
import com.vk.api.sdk.objects.likes.Type;
import com.vk.api.sdk.objects.photos.PhotoSizesType;
import com.vk.api.sdk.objects.wall.GetFilter;
import com.vk.api.sdk.objects.wall.Wallpost;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.aboba.utils.CheckContentType.isValidImageType;
import static com.aboba.utils.CheckContentType.isValidVideoType;

@Service
@Transactional
public class VkService {
    private static final Integer SCOPES_BIT_MASK = 140487903;
    private final VkAccountRepository vkRepository;
    private final VkMetricService vkMetricService;
    private final KeyCloakService keyCloakService;
    private final ScheduledStoriesRepository scheduledStoriesRepository;
    private final RepostedGroupRepository repostedGroupRepository;
    private final ThemeRepository themeRepository;
    private final WordRepository wordRepository;
    private final ParsedGroupRepository parsedGroupRepository;
    private final ParsedGroupMemberRepository parsedGroupMemberRepository;
    private final ScheduledPostRepository scheduledPostRepository;
    private final S3Service s3Service;
    private final ImageService imageService;

    @SuppressWarnings("checkstyle:MemberName")
    @Value("${vk1.client_secret}")
    private String CLIENT_SECRET;
    @SuppressWarnings("checkstyle:MemberName")
    @Value("${vk1.client_id}")
    private Integer CLIENT_ID;
    @SuppressWarnings("checkstyle:MemberName")
    @Value("${vk1.client_redirect_url}")
    private String REDIRECT_URL;
    @SuppressWarnings("checkstyle:MemberName")
    @Value("${vk1.authentication_url}")
    private String AUTHORIZATION_QUERY_USER;

    @Value("${s3.url}")
    private String s3Url;

    public VkService(
            VkAccountRepository vkRepository,
            KeyCloakService keyCloakService,
            ScheduledStoriesRepository scheduledStoriesRepository,
            RepostedGroupRepository repostedGroupRepository,
            ThemeRepository themeRepository,
            WordRepository wordRepository,
            ParsedGroupRepository parsedGroupRepository,
            ParsedGroupMemberRepository parsedGroupMemberRepository,
            VkMetricService vkMetricService,
            ScheduledPostRepository scheduledPostRepository,
            S3Service s3Service,
            ImageService imageService
    ) {
        this.vkRepository = vkRepository;
        this.keyCloakService = keyCloakService;
        this.scheduledStoriesRepository = scheduledStoriesRepository;
        this.repostedGroupRepository = repostedGroupRepository;
        this.themeRepository = themeRepository;
        this.wordRepository = wordRepository;
        this.parsedGroupRepository = parsedGroupRepository;
        this.parsedGroupMemberRepository = parsedGroupMemberRepository;
        this.vkMetricService = vkMetricService;
        this.scheduledPostRepository = scheduledPostRepository;
        this.s3Service = s3Service;
        this.imageService = imageService;
    }

    public void editScheduledPost(VkAccount credentials, WallPostRequest request)
            throws ClientException, ApiException, WrongBodyStructureException, EntityNotFoundException {
        var vk = new VkApiClient(new HttpTransportClient());
        var actor = createUserActor(credentials);
        if (request.getPostId() == null) {
            throw new WrongBodyStructureException();
        }
        var postsList = scheduledPostRepository.findByPostId(request.getPostId());
        if (postsList.isEmpty()) {
            throw new EntityNotFoundException("ScheduledPost");
        }
        var post = postsList.get(0);

        var publishedDate = post.getScheduledDate();
        var attachments = post.getAttachments();
        var message = post.getText();

        if (request.getImages() != null) {
            attachments = request.getImages();
        }
        if (request.getMessage() != null) {
            message = request.getMessage();
        }
        if (request.getPublishedDate() != null) {
            publishedDate = (int) DateTimeParseHelper.fromStringToEpochSeconds(request.getPublishedDate());
        }
        vk.wall().edit(actor, request.getPostId()).publishDate(publishedDate).attachments(attachments).message(message)
                .execute();

        post.setText(message);
        post.setScheduledDate(publishedDate);

        if (request.getImages() != null) {
            var response = vk.wall().get(actor).ownerId(credentials.getId())
                    .filter(GetFilter.POSTPONED)
                    .execute().getItems().stream()
                    .filter(postItem -> postItem.getId().equals(request.getPostId())).findFirst()
                    .get();

            post.updateAttachments(response, credentials.getId());
        }
    }

    public PaginateResponse<ScheduledPost> getScheduledPosts(VkAccount credentials, Pageable pageable) {
        var page = scheduledPostRepository.findAllByAccountSettings(credentials.getSettings(), pageable);
        return new PaginateResponse<>(page);
    }

    public void addNewPostToWall(VkAccount credentials, WallPostRequest request) throws ClientException, ApiException {
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(credentials.getId(), credentials.getAccessToken());

        String message = "";
        var attachments = new ArrayList<String>();
        int time = (int) DateTimeParseHelper.fromStringToEpochSeconds(request.getPublishedDate());
        if (request.getImages() != null) {
            attachments.addAll(request.getImages());
        }
        if (request.getVideos() != null) {
            attachments.addAll(request.getVideos());
        }
        if (request.getMessage() != null) {
            message = request.getMessage();
        }

        var postId =
                vk.wall().post(actor).publishDate(time).attachments(attachments).message(message).execute().getPostId();
        var response = vk.wall().get(actor).ownerId(credentials.getId())
                .filter(GetFilter.POSTPONED)
                .execute().getItems().stream()
                .filter(post -> post.getId().equals(postId)).findFirst()
                .get();

        scheduledPostRepository.save(new ScheduledPost(response, credentials));
    }

    public List<VkAccountResponse> getAvailableAccounts(User user) {
        return user.getVkAccounts().stream()
                .map(account -> new VkAccountResponse(account.getId(), account.getFullName())).toList();
    }

    public String addVkAccount() {
        return getUserAuthUrl(null);
    }

    public void saveGroupMembersToDatabaseByBatch(
            VkAccount credentials,
            String groupUrl
    ) throws ClientException, ApiException, InterruptedException {
        var vk = new VkApiClient(new HttpTransportClient());
        var actor = createUserActor(credentials);
        var vkSettings = credentials.getSettings();
        var groupShortName = parseGroupLink(groupUrl);

        getGroupMembersPartByQuery(vk, actor, groupShortName, 1, 0);
        var groupInfo =
                vk.groups().getByIdObjectLegacy(actor).fields(Fields.MEMBERS_COUNT).groupId(groupShortName).execute()
                        .get(0);

        var groupId = groupInfo.getId();
        var optionalGroup = parsedGroupRepository.findById(groupId);

        ParsedGroup group;
        if (optionalGroup.isPresent()) {
            group = optionalGroup.get();
            group.addVkSettings(vkSettings);
        } else {
            group = new ParsedGroup(groupInfo);
            group.addVkSettings(vkSettings);
        }

        new DownloadThread("downloadThread", groupShortName, credentials, groupInfo, group).start();
    }

    public void downloadGroupMembers(VkAccount credentials, GetByIdObjectLegacyResponse groupInfo,
                                     String groupShortName, ParsedGroup group)
            throws ClientException, InterruptedException, ApiException {
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        var actor = createUserActor(credentials);

        var countOfGroupMembers = groupInfo.getMembersCount();
        var groupId = groupInfo.getId();

        parsedGroupMemberRepository.deleteAllByGroupId(groupId);
        var countOfReceivedMembers = 0;
        var countParameterOfQuery = 1000;

        while (countOfReceivedMembers < countOfGroupMembers) {
            if (countOfGroupMembers - countOfReceivedMembers < countParameterOfQuery) {
                countParameterOfQuery = countOfGroupMembers - countOfReceivedMembers;
            }
            var receivedMembers =
                    getGroupMembersPartByQuery(vk, actor, groupShortName, countParameterOfQuery,
                            countOfReceivedMembers).stream().map(member -> new ParsedGroupMember(member, group))
                            .toList();
            countOfReceivedMembers = countOfReceivedMembers + countParameterOfQuery;

            parsedGroupMemberRepository.saveAll(receivedMembers);
            var groupAgain = parsedGroupRepository.findById(groupId).get();
            groupAgain.setUpdatedNow();
            parsedGroupRepository.save(groupAgain);
        }
    }

    public Page<ParsedGroup> getParsedGroups(VkAccount credentials, Pageable pageable) {
        var vkSettings = credentials.getSettings();
        return parsedGroupRepository.findAllByAccountSettings(vkSettings, pageable);
    }

    public PaginateResponse<ParsedGroupMember> getParsedGroupMembers(
            Integer groupId,
            Pageable pageable,
            Integer sex,
            Integer from,
            Integer to,
            Integer country,
            Integer city,
            ELastSeen lastSeen
    ) throws EntityNotFoundException {
        var groupDoesNotExists = !parsedGroupRepository.existsById(groupId);
        if (groupDoesNotExists) {
            throw new EntityNotFoundException("ParsedGroup");
        }
        if (isDownloading(groupId)) {
            return new PaginateResponse<>(0, new ArrayList<>());
        }
        BooleanBuilder query = new BooleanBuilder();
        query.and(QParsedGroupMember.parsedGroupMember.group.id.eq(groupId));

        if (country != null) {
            query.and(QParsedGroupMember.parsedGroupMember.country.eq(country));
            if (city != null) {
                query.and(QParsedGroupMember.parsedGroupMember.city.eq(city));
            }
        }
        if (sex != null) {
            query.and(QParsedGroupMember.parsedGroupMember.sex.eq(sex));
        }
        if (from != null) {
            var fromDate = ZonedDateTime.now().minusYears(from).toLocalDate();
            query.and(QParsedGroupMember.parsedGroupMember.bDate.before(fromDate));
        }
        if (to != null) {
            var toDate = ZonedDateTime.now().minusYears(to).toLocalDate();
            query.and(QParsedGroupMember.parsedGroupMember.bDate.after(toDate));
        }
        if (lastSeen != null) {
            var startOfToday = LocalDate.now().atStartOfDay();
            switch (lastSeen) {
                case TODAY -> query.and(QParsedGroupMember.parsedGroupMember.lastSeen.after(startOfToday));
                case YESTERDAY -> {
                    var startOfYesterday = startOfToday.minusDays(1);
                    query.and(QParsedGroupMember.parsedGroupMember.lastSeen.between(startOfYesterday, startOfToday));
                }
                case WEEK -> {
                    var startOfWeek = startOfToday.minusWeeks(1);
                    var startOfTodayMinusTwoDays = startOfToday.minusDays(2);
                    query.and(QParsedGroupMember.parsedGroupMember.lastSeen.between(startOfWeek,
                            startOfTodayMinusTwoDays));
                }
            }
        }

        Page<ParsedGroupMember> page = parsedGroupMemberRepository.findAll(query, pageable);
        int count = page.getTotalPages();
        List<ParsedGroupMember> response = page.stream().toList();

        return new PaginateResponse<>(count, response);
    }

    public List<Country> getCountries(VkAccount credentials) throws ClientException, ApiException {
        var vk = new VkApiClient(new HttpTransportClient());
        var actor = createUserActor(credentials);
        var response = vk.database().getCountries(actor).count(250).needAll(true).execute();

        return response.getItems();
    }

    public List<City> getCities(VkAccount credentials, int countryId, String q)
            throws ClientException, ApiException {
        var vk = new VkApiClient(new HttpTransportClient());
        var actor = createUserActor(credentials);
        var response = vk.database().getCities(actor, countryId).count(1000).needAll(true).q(q).execute();

        return response.getItems();
    }

    public void subscribeGroup(
            VkAccount credentials,
            String domain
    ) throws ClientException, ApiException, InterruptedException, MismatchEnumException {
        var vkId = credentials.getId();
        var settings = credentials.getSettings();
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(vkId, credentials.getAccessToken());
        var group =
                vk.groups().getByIdObjectLegacy(actor).groupId(domain).fields().execute()
                        .get(0);
        vk.groups().join(actor).groupId(group.getId()).execute();
        Thread.sleep(200);
        settings.addSubscribedGroup(new SubscribedGroup(group.getId()));
        var commenting = settings.isCommentingPostsWhenSubscribeGroup();
        var liking = settings.isLikingPostsWhenSubscribeGroup();

        if (commenting || liking) {
            var postIds = getLastThreePostsIdsInSubscribedGroup(vk, actor, group.getId());
            if (commenting) {
                var random = new Random();
                var comments = settings.getGroupPostComments();
                if (comments.size() > 0) {
                    for (var postId : postIds) {
                        int commentNumber = random.nextInt(comments.size());
                        vk.wall().createComment(actor, postId).ownerId(-group.getId())
                                .message(comments.get(commentNumber).getText()).execute();
                        vkMetricService.createCommentMetric(vkId);
                        Thread.sleep(200);
                    }
                }
            }
            if (liking) {
                for (var postId : postIds) {
                    vk.likes().add(actor, Type.POST, postId).ownerId(-group.getId()).execute();
                    vkMetricService.createLikeMetric(vkId);
                }
            }
        }
    }

    private List<Integer> getLastThreePostsIdsInSubscribedGroup(VkApiClient vk, UserActor actor, Integer groupId)
            throws ClientException, ApiException, InterruptedException {
        var postIds = vk.wall().get(actor).ownerId(-groupId).count(3).execute()
                .getItems()
                .stream()
                .map(Wallpost::getId)
                .toList();
        Thread.sleep(200);

        return postIds;
    }

    public List<GroupResponse> guessTheCategory(
            VkAccount credentials,
            String url
    ) throws VkNotEnoughRightsException, IOException, ClientException, ApiException {
        int perPage = 20;
        var groupDomain = parseGroupLink(url);
        var words = getKeyWords(credentials, groupDomain);

        var dictionary = wordRepository.findAllWordFields();
        words = words.stream().filter(dictionary::contains).toList();
        var themes = themeRepository.findAllByWordsWordIn(words).stream().map(Theme::getTheme).collect(
                Collectors.toSet());
        var rawThemes = new HashSet<>(themeRepository.findAllByWordsWordIn(words));

        var result = new ArrayList<GroupResponse>(themes.size() * perPage);
        for (var theme : rawThemes) {
            var query = UriComponentsBuilder.fromUriString(
                            "https://vk.com/groups?act=catalog&c[section]=communities&c[type]=4&c[skip_catalog]=1")
                    .queryParam("c[per_page]", perPage)
                    .queryParam("c[sort]", 6)
                    .queryParam("c[theme]", theme.getCode())
                    .build().toString();
            result.addAll(parseHtml(query));
        }

        return result;
    }

    public boolean toggleAutoRepostGroup(Long autoRepostGroupId) throws EntityNotFoundException {
        var group = getAutoRepostGroup(autoRepostGroupId);
        var newValue = !group.isAutoReposted();
        group.setAutoReposted(newValue);
        repostedGroupRepository.save(group);

        return newValue;
    }

    public RepostedGroup getAutoRepostGroup(Long autoRepostGroupId) throws EntityNotFoundException {
        var optionalGroup = repostedGroupRepository.findById(autoRepostGroupId);
        if (optionalGroup.isEmpty()) {
            throw new EntityNotFoundException("AutoRepostGroup");
        }

        return optionalGroup.get();
    }

    public void addNewGroupForRepost(
            VkAccount credentials,
            String url
    ) throws ClientException, ApiException, VkNotEnoughRightsException {
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(credentials.getId(), credentials.getAccessToken());
        var vkSettings = credentials.getSettings();
        if (!vkSettings.isRepostingNewPostsInGroups()) {
            throw new VkNotEnoughRightsException();
        }
        var groupShortName = parseGroupLink(url);
        //maybe Fields.DESCRIPTION, Fields.STATUS,
        var group =
                vk.groups().getByIdObjectLegacy(actor).groupId(groupShortName).fields(Fields.MEMBERS_COUNT).execute()
                        .get(0);
        vkSettings.getRepostedGroups().add(new RepostedGroup(group, url, vkSettings));

        vkRepository.save(credentials);
    }

    public List<RepostedGroup> getAllRepostedGroups(VkAccount credentials) {
        return credentials.getSettings().getRepostedGroups();
    }

    public boolean toggleLikingNewPostsInGroups(VkAccount credentials) {
        boolean newValue = !credentials.getSettings().isLikingNewPostsInGroups();
        credentials.getSettings().setLikingNewPostsInGroups(newValue);
        vkRepository.save(credentials);

        return newValue;
    }

    public boolean toggleLikingNewPhotosOfFriends(VkAccount credentials) {
        boolean newValue = !credentials.getSettings().isLikingNewPhotosOfFriends();
        credentials.getSettings().setLikingNewPhotosOfFriends(newValue);
        vkRepository.save(credentials);

        return newValue;
    }

    public boolean toggleCommentingNewPostsInGroups(VkAccount credentials) {
        boolean newValue = !credentials.getSettings().isCommentingNewPostsInGroups();
        credentials.getSettings().setCommentingNewPostsInGroups(newValue);
        vkRepository.save(credentials);

        return newValue;
    }

    public boolean toggleLikingPostsWhenSubscribeGroup(VkAccount credentials) {
        boolean newValue = !credentials.getSettings().isLikingPostsWhenSubscribeGroup();
        credentials.getSettings().setLikingPostsWhenSubscribeGroup(newValue);
        vkRepository.save(credentials);

        return newValue;
    }

    public boolean toggleLikingPhotosWhenApproveFriendship(VkAccount credentials) {
        boolean newValue = !credentials.getSettings().isLikingPhotosWhenApproveFriendship();
        credentials.getSettings().setLikingPhotosWhenApproveFriendship(newValue);
        vkRepository.save(credentials);

        return newValue;
    }

    public boolean toggleRepostingNewPostsInGroups(VkAccount credentials) {
        boolean newValue = !credentials.getSettings().isRepostingNewPostsInGroups();
        credentials.getSettings().setRepostingNewPostsInGroups(newValue);
        vkRepository.save(credentials);

        return newValue;
    }

    public boolean toggleCommentingPostsWhenSubscribeGroup(VkAccount credentials) {
        boolean newValue = !credentials.getSettings().isCommentingPostsWhenSubscribeGroup();
        credentials.getSettings().setCommentingPostsWhenSubscribeGroup(newValue);
        vkRepository.save(credentials);

        return newValue;
    }

    public boolean toggleCommentingPhotosWhenApproveFriendship(VkAccount credentials) {
        boolean newValue = !credentials.getSettings().isCommentingPhotosWhenApproveFriendship();
        credentials.getSettings().setCommentingPhotosWhenApproveFriendship(newValue);
        vkRepository.save(credentials);

        return newValue;
    }

    public boolean toggleApprovingFriendship(VkAccount credentials) {
        boolean newValue = !credentials.getSettings().isApprovingFriendship();
        credentials.getSettings().setApprovingFriendship(newValue);
        vkRepository.save(credentials);

        return newValue;
    }

    public String uploadVideo(VkAccount credentials, MultipartFile video)
            throws WrongContentTypeException, ClientException, ApiException, IOException {
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(credentials.getId(), credentials.getAccessToken());
        if (!isValidVideoType(video.getContentType())) {
            throw new WrongContentTypeException();
        }
        File videoFile = toFile(video);

        var uploadUrl = vk.videos().save(actor).execute().getUploadUrl().toString();
        var uploadResponse = vk.upload().video(uploadUrl, videoFile).execute();
        videoFile.delete();

        return String.format("video%d_%d", actor.getId(), uploadResponse.getVideoId());
    }

    public PaginateResponse<StoriesDTO> getScheduledStories(VkAccount account, Pageable pageable) {
        var page = scheduledStoriesRepository.findAllByAccount(account, pageable);
        var totalPages = page.getTotalPages();
        var list = page.stream().map(stories -> new StoriesDTO(stories, s3Url)).toList();
        return new PaginateResponse<>(totalPages, list);
    }

    private ScheduledStories findScheduledStoriesByIdAndAccount(VkAccount account, Long storiesId)
            throws EntityNotFoundException {
        var optionalScheduledStories = scheduledStoriesRepository.findByIdAndAccount(storiesId, account);
        if (optionalScheduledStories.isEmpty()) {
            throw new EntityNotFoundException("ScheduledStories");
        }
        return optionalScheduledStories.get();
    }

    public StoriesDTO getScheduledStoriesByIdAndAccount(VkAccount account, Long storiesId)
            throws EntityNotFoundException {
        var stories = findScheduledStoriesByIdAndAccount(account, storiesId);

        return new StoriesDTO(stories, s3Url);
    }

    public UploadStoriesResultDTO uploadStories(VkAccount credentials, MultipartFile media)
            throws WrongContentTypeException, ClientException, ApiException, IOException, WrongBodyStructureException {
        String contentType = Optional.ofNullable(media.getContentType()).orElseThrow(WrongBodyStructureException::new);
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(credentials.getId(), credentials.getAccessToken());

        var uploadResult = "";
        var s3Path = "";
        if (isValidImageType(contentType)) {
            uploadResult = uploadPhotoStoriesToVk(vk, actor, media);
            s3Path = uploadPhotoStoriesToS3(uploadResult, media);
        } else if (isValidVideoType(contentType)) {
            uploadResult = uploadVideoStoriesToVk(vk, actor, media);
            s3Path = uploadVideoStoriesToS3(uploadResult, media);
        } else {
            throw new WrongContentTypeException();
        }
        var url = String.format("%s/stories/%s", s3Url, s3Path);
        return new UploadStoriesResultDTO(contentType, uploadResult, url);
    }

    private String uploadPhotoStoriesToVk(VkApiClient vk, UserActor actor, MultipartFile media)
            throws ClientException, ApiException, IOException {
        var mediaFile = toFile(media);
        try {
            var uploadUrl = vk.stories().getPhotoUploadServer(actor).addToNews(true).execute().getUploadUrl();
            return vk.upload().photoStory(uploadUrl, mediaFile).execute().getUploadResult();
        } catch (Exception e) {
            throw e;
        } finally {
            mediaFile.delete();
        }
    }

    private String uploadVideoStoriesToVk(VkApiClient vk, UserActor actor, MultipartFile media)
            throws ClientException, ApiException, IOException {
        var mediaFile = toFile(media);
        try {
            var uploadUrl = vk.stories().getVideoUploadServer(actor).addToNews(true).execute().getUploadUrl();
            return vk.upload().videoStory(uploadUrl, mediaFile).execute().getUploadResult();
        } catch (Exception e) {
            throw e;
        } finally {
            mediaFile.delete();
        }
    }

    private String uploadPhotoStoriesToS3(String uploadResponse, MultipartFile media) throws IOException {
        var is = media.getInputStream();
        var os = imageService.makeImage(is, EImage.STORIES);

        return s3Service.uploadImage(uploadResponse, os, EImage.STORIES, "image");
    }

    private String uploadVideoStoriesToS3(String uploadResponse, MultipartFile media) throws IOException {
        var size = media.getSize();
        var is = media.getInputStream();
        return s3Service.uploadVideo(uploadResponse, is, size, EVideo.STORIES, "video", media.getContentType());
    }

//    public void editStories(VkAccount account, StoriesPostRequest request)
//            throws WrongBodyStructureException, EntityNotFoundException {
//        if (request.getId() == null) {
//            throw new WrongBodyStructureException();
//        }
//        var stories = scheduledStoriesRepository
//                .findByIdAndAccount(request.getId(), account)
//                .orElseThrow(() -> new EntityNotFoundException("ScheduledStories"));
//
//        if (request.getPublishedDate() != null) {
//            var publishDate = fromStringToLocalDateTime(request.getPublishedDate());
//            var now = LocalDateTime.now();
//            if (publishDate.isBefore(now)) {
//                throw new WrongBodyStructureException();
//            }
//            stories.setPublishDate(publishDate);
//        }
//        if (request.getStories() != null) {
//            stories.updateUploadResults(request.getStories());
//        }
//        scheduledStoriesRepository.save(stories);
//
//    }

    public void editStories(VkAccount account, StoriesDTO request)
            throws WrongBodyStructureException, EntityNotFoundException {
        if (request.getId() == null) {
            throw new WrongBodyStructureException();
        }
        var stories = scheduledStoriesRepository
                .findByIdAndAccount(request.getId(), account)
                .orElseThrow(() -> new EntityNotFoundException("ScheduledStories"));

        if (request.getPublishedDate() != null) {
            var publishDate = DateTimeParseHelper.fromStringToLocalDateTime(request.getPublishedDate());
            var now = LocalDateTime.now();
            if (publishDate.isBefore(now)) {
                throw new WrongBodyStructureException();
            }
            stories.setPublishDate(publishDate);
        }
        if (request.getStoriesUploadResult() != null && request.getContentType() != null) {
            var contentType = request.getContentType();

            if (!(isValidImageType(contentType) || isValidVideoType(contentType))) {
                throw new WrongBodyStructureException();
            }
            stories.setUploadResult(request.getStoriesUploadResult());
            stories.setContentType(contentType);
        }
        scheduledStoriesRepository.save(stories);
    }

    public List<String> saveStories(VkAccount account, List<StoriesDTO> listOfStories)
            throws ClientException, ApiException {
        List<String> scheduledStories = new ArrayList<>();
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(account.getId(), account.getAccessToken());

        for (var stories : listOfStories) {
            var uploadResult = stories.getStoriesUploadResult();
            if (stories.getPublishedDate() != null) {
                var publishedDate = DateTimeParseHelper.fromStringToLocalDateTime(stories.getPublishedDate());
                if (publishedDate.isAfter(LocalDateTime.now())) {
                    scheduledStoriesRepository.save(
                            new ScheduledStories(
                                    publishedDate,
                                    account,
                                    uploadResult,
                                    stories.getContentType()));
                    scheduledStories.add(uploadResult);
                }
            } else {
                vk.stories().save(actor, uploadResult).execute();
            }
        }
        return scheduledStories;
    }

    public PhotoUploadResultDTO uploadPhotos(VkAccount credentials, MultipartFile image)
            throws WrongContentTypeException, IOException, ClientException, ApiException {
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(credentials.getId(), credentials.getAccessToken());
        if (!isValidImageType(image.getContentType())) {
            throw new WrongContentTypeException();
        }
        var serverResponse = vk.photos().getWallUploadServer(actor).execute();

        File imageFile = toFile(image);

        var uploadResponse = vk.upload().photoWall(serverResponse.getUploadUrl().toString(), imageFile).execute();
        imageFile.delete();
        var photoList = vk.photos().saveWallPhoto(actor, uploadResponse.getPhoto())
                .server(uploadResponse.getServer())
                .hash(uploadResponse.getHash())
                .execute();
        var photo = photoList.get(0);
        var uploadedPhoto =
                photo.getSizes().stream().filter(size -> size.getType().equals(PhotoSizesType.Q)).findFirst().get();
        return new PhotoUploadResultDTO(uploadedPhoto, String.format("photo%d_%d", photo.getOwnerId(), photo.getId()));
    }

    public GroupCommentsDTO getGroupComments(VkAccount credentials) {
        var settings = credentials.getSettings();
        var texts = settings.getGroupPostComments().stream().map(Comment::getText).toList();
        return new GroupCommentsDTO(texts, settings.isCommentingNewPostsInGroups());
    }

    public FriendCommentsDTO getFriendComments(VkAccount credentials) {
        var settings = credentials.getSettings();
        var texts = settings.getFriendPhotoComments().stream().map(Comment::getText).toList();
        return new FriendCommentsDTO(texts, settings.isLikingNewPhotosOfFriends());
    }

    public void patchGroupComments(VkAccount credentials, GroupCommentsDTO request) {
        var settings = credentials.getSettings();
        settings.setCommentingNewPostsInGroups(request.isCommentingNewPostsInGroups());
        settings.deleteAllGroupPostComments();
        settings.setGroupPostComments(request.getComments());
    }

    public void patchFriendComments(VkAccount credentials, FriendCommentsDTO request) {
        var settings = credentials.getSettings();
        settings.setLikingNewPhotosOfFriends(request.isLikingNewPhotosOfFriends());
        settings.deleteAllFriendPhotoComments();
        settings.setFriendPhotoComments(request.getComments());
    }

    public void saveCredentials(CredentialsSaveRequest request, User user) throws ClientException, ApiException {
        var id = request.getId();
        var accessToken = request.getAccessToken();
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(id, accessToken);
        var info = vk.account().getProfileInfo(actor).execute();
        var fullName = String.format("%s %s", info.getFirstName(), info.getLastName());

        vkRepository.save(new VkAccount(user, id, accessToken, fullName));
    }

    private File toFile(MultipartFile media) throws IOException {
        InputStream is = media.getInputStream();
        byte[] buffer = new byte[is.available()];
        is.read(buffer);

        File file = new File(
                String.format("src/main/resources/%s.%s", UUID.randomUUID(), media.getContentType().substring(6)));
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(buffer);
        }
        return file;
    }

    private String getUserAuthUrl(String link) {
        return String.format(AUTHORIZATION_QUERY_USER, CLIENT_ID, REDIRECT_URL, SCOPES_BIT_MASK, link);
    }

    private boolean isDownloading(Integer groupId) {
        var Threads = Thread.getAllStackTraces().keySet();
        for (var thread : Threads) {
            if (thread instanceof DownloadThread downloadThread) {
                if (downloadThread.getGroupId().equals(groupId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<GroupResponse> parseHtml(String url) throws IOException {
        var doc = Jsoup.connect(url).get();
        var listOfGroups = new ArrayList<GroupResponse>();
        var groups = doc.getElementsByClass("groups_row search_row clear_fix");
        for (var group : groups) {
            var img = group.getElementsByClass("search_item_img").get(0).attr("src");
            var divInfo = group.getElementsByClass("info").get(0);
            var aNode = divInfo.getElementsByClass("labeled title verified_label").first().firstChild();
            var shortName = aNode.attr("href");
            var name = aNode.firstChild().toString();
            if (name.startsWith("\n")) {
                name = name.substring(1);
            }
            var category = group.getElementsByClass("labeled ").first().firstChild().toString();
            if (category.startsWith("\n")) {
                category = category.substring(1);
            }
            Element membersInfo = category.equals("Закрытая группа") ?
                    divInfo.getElementsByClass("labeled ").first() :
                    divInfo.getElementsByClass("labeled labeled_link").first();
            membersInfo.children().remove();
            var nodes = membersInfo.childNodes();
            var members = nodes.stream().map(Node::toString).collect(Collectors.joining(""));
            if (members.startsWith("\n")) {
                members = members.substring(1);
            }
            listOfGroups.add(new GroupResponse(img, name, category, members, shortName));
        }

        return listOfGroups;
    }

    private List<String> getKeyWords(VkAccount credentials, String groupDomain)
            throws VkNotEnoughRightsException, IOException, ClientException, ApiException {
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(credentials.getId(), credentials.getAccessToken());
        var vkSettings = credentials.getSettings();
        if (!vkSettings.isRepostingNewPostsInGroups()) {
            throw new VkNotEnoughRightsException();
        }
        var group = vk.groups().getByIdObjectLegacy(actor).groupId(groupDomain)
                .fields(Fields.DESCRIPTION, Fields.MEMBERS_COUNT, Fields.STATUS).execute().get(0);

        Analyzer analyzer = new RussianAnalyzer();
        List<String> words = new ArrayList<>();
        TokenStream tokenStream = analyzer.tokenStream("field",
                String.format("%s %s %s", group.getName(), group.getStatus(), group.getDescription()));
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            words.add(attr.toString());
        }

        return words;
    }

    private String parseGroupLink(String url) {
        return url.substring(15);
    }

    private UserActor createUserActor(VkAccount credentials) {
        var userId = credentials.getId();
        var token = credentials.getAccessToken();

        return new UserActor(userId, token);
    }

    private List<UserXtrRole> getGroupMembersPartByQuery(
            VkApiClient vk,
            UserActor actor,
            String shortName,
            int countParameterOfQuery,
            int countOfReceivedMembers
    ) throws ClientException, ApiException, InterruptedException {
        var result = vk.groups()
                .getMembersWithFields(
                        actor,
                        com.vk.api.sdk.objects.users.Fields.SEX,
                        com.vk.api.sdk.objects.users.Fields.BDATE,
                        com.vk.api.sdk.objects.users.Fields.COUNTRY,
                        com.vk.api.sdk.objects.users.Fields.CITY,
                        com.vk.api.sdk.objects.users.Fields.LAST_SEEN,
                        com.vk.api.sdk.objects.users.Fields.PHOTO_100
                )
                .groupId(shortName)
                .count(countParameterOfQuery)
                .offset(countOfReceivedMembers)
                .execute()
                .getItems();
        Thread.sleep(500);

        return result;
    }

    private LocalDate parseVkDateToLocalDate(String vkBDate) {
        if (vkBDate == null) {
            return null;
        }
        var array = vkBDate.split("[.]");
        if (array.length != 3) {
            return null;
        }
        if (array[0].length() == 1) {
            array[0] = String.format("0%s", array[0]);
        }
        if (array[1].length() == 1) {
            array[1] = String.format("0%s", array[1]);
        }
        var finalDate = String.format("%s.%s.%s", array[0], array[1], array[2]);
        var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        return LocalDate.parse(finalDate, formatter);
    }

    //--------------------------- CALLING BY ANNOTATIONS ------------------------------------

    //calling by annotations
    public VkAccount getStandaloneCredentials() throws NotAuthorizedException, VkUserNotAuthorizedException {
        String userId = keyCloakService.getCurrentUserId();
        Optional<VkAccount> optionalVkCredentials = vkRepository.findByUserId(userId);
        if (optionalVkCredentials.isEmpty()) {
            throw new VkUserNotAuthorizedException(getUserAuthUrl(null));
        }

        return optionalVkCredentials.get();
    }

    //calling by VkCredentialsAspect
    public VkAccount getAccountByVkId(Integer vkId) throws NotAuthorizedException, VkUserNotAuthorizedException {
        if (vkId == null) {
            throw new VkUserNotAuthorizedException(getUserAuthUrl(null));
        }
        String userId = keyCloakService.getCurrentUserId();

        return vkRepository.findByIdAndUserId(vkId, userId)
                .orElseThrow(() -> new VkUserNotAuthorizedException(getUserAuthUrl(null)));
    }

    //--------------------------- SITE ------------------------------------

//    //calling by annotations
//    public VkAccount getSiteUserCredentials() throws VkUserNotAuthorizedException, NotAuthorizedException {
//        String userId = keyCloakService.getCurrentUserId();
//        Optional<VkAccount> optionalVkCredentials = vkRepository.findByUserId(userId);
//        if (optionalVkCredentials.isEmpty()) {
//            throw new VkUserNotAuthorizedException(REDIRECT_URL);
//        }
//
//        return optionalVkCredentials.get();
//    }
//
//    //calling by annotations
//    public List<ManagedGroup> getSiteGroupsCredentials()
//            throws VkUserNotAuthorizedException, NotAuthorizedException, ClientException, ApiException,
//            VkGroupsNotAuthorizedException {
//        var credentials = getSiteUserCredentials();
//        List<Integer> ids = getGroupsIds(credentials);
//        List<ManagedGroup> managedGroups = credentials.getSettings().getManagedGroups();
//        List<Integer> idsFromDb = managedGroups.stream().map(ManagedGroup::getId).toList();
//        if (!ids.equals(idsFromDb)) {
//            throw new VkGroupsNotAuthorizedException(REDIRECT_URL);
//        }
//
//        return managedGroups;
//    }
//
//    private List<Integer> getGroupsIds(VkAccount credentials) throws ClientException, ApiException {
//        VkApiClient vk = new VkApiClient(new HttpTransportClient());
//        UserActor actor = new UserActor(credentials.getId(), credentials.getAccessToken());
//        return vk.groups()
//                .get(actor).filter(Filter.ADMIN)
//                .execute()
//                .getItems();
//    }
}
