package com.aboba.vk.main.controller;

import com.aboba.annotation.SiteVkGroupsCredentials;
import com.aboba.domain.vk.main.entity.ManagedGroup;
import com.aboba.domain.vk.main.entity.VkAccount;
import com.aboba.domain.vk.main.repository.VkAccountRepository;
import com.aboba.exception.vk.VkUserNotAuthorizedException;
import com.aboba.utils.dto.Response;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.UserAuthResponse;
import com.vk.api.sdk.objects.groups.Filter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("v1/old/vk")
@Tag(name = "v1/old/vk", description = "vk API")
public class VKControllerOld {
    private static final String AUTHORIZATION_QUERY_USER =
            "https://oauth.vk.com/authorize?client_id=%s&display=page&redirect_uri=%s&scope=%s&response_type=code&state=%s&v=5.131";
    private static final String AUTHORIZATION_QUERY_GROUPS =
            "https://oauth.vk.com/authorize?client_id=%s&display=page&redirect_uri=%s&group_ids=%s&scope=%s&response_type=code&v=5.131";
    private static final Integer USER_SCOPES_BIT_MASK = 140455135;
    private static final Integer GROUPS_SCOPES_BIT_MASK = 397317;
    private final VkAccountRepository vkRepository;
    @Value("${vk.app_id}")
    private Integer APP_ID;
    @Value("${vk.client_secret}")
    private String CLIENT_SECRET;
    @Value("${vk.redirect_url_user}")
    private String REDIRECT_URL_USER;
    @Value("${vk.redirect_url_groups}")
    private String REDIRECT_URL_GROUPS;

    public VKControllerOld(VkAccountRepository vkRepository) {
        this.vkRepository = vkRepository;
    }

    @GetMapping("/test")
    public ResponseEntity<Object> test(
            @SiteVkGroupsCredentials List<ManagedGroup> managedGroups
    ) throws ClientException, ApiException, IOException, VkUserNotAuthorizedException {
        managedGroups.forEach(groupCredential -> {
            System.out.println("groupId: " + groupCredential.getId());
            System.out.println("accessToken: " + groupCredential.getAccessToken());
        });
        return ResponseEntity.ok(new Response<>(true, "success"));
    }

//    @GetMapping("/callback/user")  // TODO: 12.07.2022 for postman
//    public ResponseEntity<String> callbackUser(
////            @CurrentUser User user,
//            @RequestParam(required = false) String link,
//            @RequestParam(required = false) String code,
//            @RequestParam(required = false) String state
//    ) throws ClientException, ApiException {
//        if (code == null) {
//            var authUrl = getUserAuthUrl(link);
//            return ResponseEntity.ok(authUrl);
//        }
//        String clientId = "a31a1dd1-9066-48c7-b142-2daf8eb06c7d";
//        var userAuthResponse = authUser(code);
//        vkRepository.save(new VkAccount(clientId, userAuthResponse.getUserId(), userAuthResponse.getAccessToken()));
//
//        return ResponseEntity.ok(state);
//    }

    @GetMapping("/callback/groups")  // TODO: 12.07.2022 for postman
    public ResponseEntity<String> callbackGroups(
//            @CurrentUser User user,
            @RequestParam(required = false) String link,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state
    ) throws ClientException, ApiException {
        if (code == null) {
            String userId = "a31a1dd1-9066-48c7-b142-2daf8eb06c7d";
            var authUrl = getGroupsAuthUrl(userId);

            return ResponseEntity.ok(authUrl);
        }
        String userId = "a31a1dd1-9066-48c7-b142-2daf8eb06c7d"; //current user id
        var accessTokens = authGroups(code);
        saveGroupsCredentials(userId, accessTokens);

        return ResponseEntity.ok(state);
    }

    private void saveGroupsCredentials(String userId, Map<Integer, String> accessTokens) {
        var credentials = vkRepository.findByUserId(userId).get();
        var settings = credentials.getSettings();
        settings.setGroupCredentials(accessTokens);
        vkRepository.save(credentials);
    }

    private List<Integer> getGroupsIds(VkAccount credentials) throws ClientException, ApiException {
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(credentials.getId(), credentials.getAccessToken());
        return vk.groups()
                .get(actor).filter(Filter.ADMIN)
                .execute()
                .getItems();
    }

    private String getStringGroupsIds(VkAccount credentials) throws ClientException, ApiException {
        var groupIds = getGroupsIds(credentials)
                .stream()
                .map(Object::toString)
                .toList();

        return String.join(",", groupIds);
    }

    private UserAuthResponse authUser(String code) throws ClientException, ApiException {
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        return vk.oAuth().userAuthorizationCodeFlow(APP_ID, CLIENT_SECRET, REDIRECT_URL_USER, code).execute();
    }

    private Map<Integer, String> authGroups(String code) throws ClientException, ApiException {
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        var groupsAuthResponse =
                vk.oAuth().groupAuthorizationCodeFlow(APP_ID, CLIENT_SECRET, REDIRECT_URL_GROUPS, code).execute();

        return groupsAuthResponse.getAccessTokens();
    }

    private String getUserAuthUrl(String link) {
        return String.format(AUTHORIZATION_QUERY_USER, APP_ID, REDIRECT_URL_USER, USER_SCOPES_BIT_MASK, link);
    }

    private String getGroupsAuthUrl(String userId) throws ClientException, ApiException {
        var credentials = vkRepository.findByUserId(userId).get();
        String idsUrlArray = getStringGroupsIds(credentials);
        return String.format(AUTHORIZATION_QUERY_GROUPS, APP_ID, REDIRECT_URL_GROUPS, idsUrlArray,
                GROUPS_SCOPES_BIT_MASK);
    }

    //    @PostMapping("/image")
//    public ResponseEntity<Response<String>> image (
//            @CurrentUser User user,
//            @RequestPart(value = "image", required = false) MultipartFile image
//    ) throws ClientException, ApiException, IOException {
//        Optional<VkCredentials> optionalCredentials = vkRepository.findByUserId(user.getId());
//        if(optionalCredentials.isEmpty()) {
//            return ResponseEntity.ok(new Response<>(false, REDIRECT_URL + "?codeType=user"));
//        } else {
//            VkCredentials credentials = optionalCredentials.get();
//            VkApiClient vk = new VkApiClient(new HttpTransportClient());
//            UserActor actor = new UserActor(credentials.getVkUserId(), credentials.getAccessToken());
//            // TODO: 14.07.2022 добавить проверку на тип
//            var serverResponse = vk.photos().getWallUploadServer(actor).execute();
//            File file = new File("src/main/resources/bufferedImage." + image.getContentType().substring(6));
//            System.out.println(image.getContentType());
//            image.transferTo(file.getAbsoluteFile());
//            var uploadResponse = vk.upload().photoWall(serverResponse.getUploadUrl().toString(), file).file(file).execute();
//            var photoList = vk.photos().saveWallPhoto(actor, uploadResponse.getPhoto())
//                    .server(uploadResponse.getServer())
//                    .hash(uploadResponse.getHash())
//                    .execute();
//
//            var photo = photoList.get(0);
//            String attachId = "photo" + photo.getOwnerId() + "_" + photo.getId();
//            var response = vk.wall().post(actor).attachments(attachId).execute();
//
//            return ResponseEntity.ok(new Response<>(true, "Added!"));
////            var getResponse = vk.wall().post(actor).attachments(attachId).execute();
//
////            UserAuthResponse authResponse = vk.photos().getUploadServer();
////            System.out.println("do something...");
////            UserActor actor = new ServiceActor(APP_ID, optionalCredentials.get().getAccessToken());
////            PhotoUpload serverResponse = vk.photos().getWallUploadServer(actor).execute();
//        }
//    }

    //    @GetMapping("/groups/all") // получение списка групп
//    public ResponseEntity<Response<Object>> authorization (
//            @CurrentUser User user
//    ) throws ClientException, ApiException {
//        Optional<VkCredentials> optionalCredentials = vkRepository.findByUserId(user.getId());
//        if(optionalCredentials.isEmpty()) {
//            return ResponseEntity.ok(new Response<>(false, REDIRECT_URL));
//        } else {
//            VkCredentials credentials = optionalCredentials.get();
//            VkApiClient vk = new VkApiClient(new HttpTransportClient());
//            UserActor actor = new UserActor(credentials.getVkUserId(), credentials.getAccessToken());
//            var groupIds = vk.groups().get(actor).filter(Filter.ADMIN).execute().getItems();
//            var groupCredentials = credentials.getGroupCredentials().stream().map(GroupCredential::getId).toList();
//            var hasToken = new ArrayList<Integer>();
//            var hasNotToken = new ArrayList<Integer>();
//            groupIds.forEach(id -> {
//                        if (groupCredentials.contains(id))
//                            hasToken.add(id);
//                        else
//                            hasNotToken.add(id);
//                    }
//            );
//
//            return ResponseEntity.ok(new Response<>(false, new GroupsResponse(hasToken, hasNotToken)));
//        }
//    }

}
