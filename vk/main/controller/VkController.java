package com.aboba.vk.main.controller;

import com.aboba.annotation.CurrentUser;
import com.aboba.annotation.VkCredentials;
import com.aboba.domain.togglz.FeatureFlags;
import com.aboba.domain.user.User;
import com.aboba.domain.vk.main.dto.*;
import com.aboba.domain.vk.main.entity.ParsedGroup;
import com.aboba.domain.vk.main.entity.ParsedGroupMember;
import com.aboba.domain.vk.main.entity.RepostedGroup;
import com.aboba.domain.vk.main.entity.VkAccount;
import com.aboba.domain.vk.main.entity.schedule.ScheduledPost;
import com.aboba.domain.vk.main.enums.ELastSeen;
import com.aboba.domain.vk.main.service.VkService;
import com.aboba.exception.EntityNotFoundException;
import com.aboba.exception.IO.WrongBodyStructureException;
import com.aboba.exception.IO.WrongContentTypeException;
import com.aboba.exception.IO.WrongParamsException;
import com.aboba.exception.MismatchEnumException;
import com.aboba.exception.vk.VkNotEnoughRightsException;
import com.aboba.swagger.vk.*;
import com.aboba.utils.dto.PaginateResponse;
import com.aboba.utils.dto.Response;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.base.Country;
import com.vk.api.sdk.objects.database.City;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("v1/vk")
@Tag(name = "v1/vk", description = "vk API")
public class VkController {

    private final VkService vkService;

    public VkController(VkService vkService) {
        this.vkService = vkService;
    }

    @GetMapping("/addVkAccount")
    @SwaggerAddVkAccountGET
    public String addVkAccount() {
        return vkService.addVkAccount();
    }

    @GetMapping("/accounts")
    @SwaggerAccountsGET
    public ResponseEntity<Response<List<VkAccountResponse>>> availableAccounts(
            @Parameter(hidden = true) @CurrentUser User user
    ) {
        var response = vkService.getAvailableAccounts(user);

        return ResponseEntity.ok(new Response<>(true, response));
    }


    @VkCredentials
    @PatchMapping("/parsedGroups/members/save")
    @SwaggerParsedGroupsMembersSavePATCH
    public ResponseEntity<Response<String>> parseGroup(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestParam String groupUrl
    ) throws ClientException, ApiException, InterruptedException {
        vkService.saveGroupMembersToDatabaseByBatch(credentials, groupUrl);

        return ResponseEntity.ok(new Response<>(true, "Downloading started."));
    }

    @VkCredentials
    @GetMapping("/parsedGroups")
    @SwaggerGroupsGET
    public ResponseEntity<Response<PaginateResponse<ParsedGroup>>> getParsedGroups(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @Parameter(hidden = true) @PageableDefault(size = 25) Pageable pageable
    ) {
        var response = vkService.getParsedGroups(credentials, pageable);

        return ResponseEntity.ok(new Response<>(true, new PaginateResponse<>(response)));
    }

    @VkCredentials
    @GetMapping("/parsedGroups/members")
    @SwaggerParsedGroupsMembersGET
    public ResponseEntity<Response<PaginateResponse<ParsedGroupMember>>> getMembersFromGroup(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @Parameter(hidden = true) @PageableDefault(size = 25) Pageable pageable,
            @RequestParam Integer groupId,
            @RequestParam(required = false) Integer sex,
            @RequestParam(required = false) Integer ageFrom,
            @RequestParam(required = false) Integer ageTo,
            @RequestParam(required = false) Integer country,
            @RequestParam(required = false) Integer city,
            @RequestParam(required = false) String lastSeen
    ) throws WrongParamsException, MismatchEnumException, EntityNotFoundException {
        ELastSeen eLastSeen = null;
        if (ageFrom != null && ageTo != null && ageFrom > ageTo) {
            throw new WrongParamsException();
        }
        if (sex != null && !(sex == 1 || sex == 2)) {
            throw new WrongParamsException();
        }
        if (lastSeen != null) {
            eLastSeen = ELastSeen.of(lastSeen);
        }
        var response =
                vkService.getParsedGroupMembers(groupId, pageable, sex, ageFrom, ageTo, country, city, eLastSeen);

        return ResponseEntity.ok(new Response<>(true, response));
    }

    @VkCredentials
    @GetMapping("/countries")
    @SwaggerCountriesGET
    public ResponseEntity<Response<List<Country>>> getCountries(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) throws ClientException, ApiException {
        var response = vkService.getCountries(credentials);
        return ResponseEntity.ok(new Response<>(true, response));
    }

    @VkCredentials
    @GetMapping("/cities")
    @SwaggerCitiesGET
    public ResponseEntity<Response<List<City>>> getCities(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestParam(required = false, defaultValue = "1") Integer countryId,
            @RequestParam(required = false, defaultValue = "") String q
    ) throws ClientException, ApiException {
        System.out.println(credentials.toString());
        var response = vkService.getCities(credentials, countryId, q);

        return ResponseEntity.ok(new Response<>(true, response));
    }

    @VkCredentials
    @GetMapping("/groups/showSimilar")
    @SwaggerShowSimilarGET
    public ResponseEntity<Response<List<GroupResponse>>> guess(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestParam String url
    ) throws IOException, VkNotEnoughRightsException, ClientException, ApiException {
        var response = vkService.guessTheCategory(credentials, url);

        return ResponseEntity.ok(new Response<>(true, response));
    }

    @VkCredentials
    @GetMapping("/groups/subscribe")
    @SwaggerGroupsSubscribeGET
    public ResponseEntity<Response<String>> subscribeGroup(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestParam String domain
    ) throws ClientException, ApiException, InterruptedException, MismatchEnumException {
        vkService.subscribeGroup(credentials, domain);

        return ResponseEntity.ok(new Response<>(true, "success"));
    }

//    @GetMapping("/luceneTest")
//    public ResponseEntity<Response<List<String>>> luceneTest(
//            @RequestParam String url,
//            @StandaloneCredentials VkCredentials credentials
//    ) throws IOException, VkNotEnoughRightsException, ClientException, ApiException {
//        var response = vkService.luceneTextParser(credentials, url);
//
//        return ResponseEntity.ok(new Response<>(true, response));
//    }

//    @GetMapping("/addToDictionary")
//    public ResponseEntity<Response<String>> addToDictionary(
//            @RequestParam String theme,
//            @RequestParam String word
//    ) {
//        vkService.addRowToDictionary(theme, word);
//        return ResponseEntity.ok(new Response<>(true, "success"));
//    }

    @VkCredentials
    @SwaggerGetScheduledPostsGET
    @GetMapping("/getScheduledPosts")
    public ResponseEntity<Response<PaginateResponse<ScheduledPost>>> getScheduledPosts(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @Parameter(hidden = true) @SortDefault(sort = "scheduledDate", direction = Sort.Direction.ASC)
            @PageableDefault(size = 25) Pageable pageable
    ) {
        var response = vkService.getScheduledPosts(credentials, pageable);
        return ResponseEntity.ok(new Response<>(true, response));
    }

    @VkCredentials
    @SwaggerEditScheduledPostsPATCH
    @PatchMapping("/editScheduledPosts")
    public ResponseEntity<Response<String>> editScheduledPost(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestBody WallPostRequest request
    ) throws WrongBodyStructureException, ClientException, ApiException, EntityNotFoundException {
        vkService.editScheduledPost(credentials, request);

        return ResponseEntity.ok(new Response<>(true, "success"));
    }

    @VkCredentials
    @SwaggerGetGroupCommentsGET
    @GetMapping("/getGroupComments")
    public ResponseEntity<Response<GroupCommentsDTO>> getGroupComments(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) {
        var response = vkService.getGroupComments(credentials);
        return ResponseEntity.ok(new Response<>(true, response));
    }

    @VkCredentials
    @SwaggerPatchGroupCommentsPATCH
    @PatchMapping("/patchGroupComments")
    public ResponseEntity<Response<String>> patchGroupComments(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestBody GroupCommentsDTO request
    ) {
        vkService.patchGroupComments(credentials, request);
        return ResponseEntity.ok(new Response<>(true, "success"));
    }

    @VkCredentials
    @SwaggerGetFriendCommentsGET
    @GetMapping("/getFriendComments")
    public ResponseEntity<Response<FriendCommentsDTO>> getFriendComments(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) {
        var response = vkService.getFriendComments(credentials);
        return ResponseEntity.ok(new Response<>(true, response));
    }

    @VkCredentials
    @SwaggerPatchFriendCommentsPATCH
    @PatchMapping("/patchFriendComments")
    public ResponseEntity<Response<String>> patchFriendComments(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestBody FriendCommentsDTO request
    ) {
        vkService.patchFriendComments(credentials, request);
        return ResponseEntity.ok(new Response<>(true, "success"));
    }

    @VkCredentials
    @PatchMapping("/changeRepostStatus")
    @SwaggerChangeRepostStatusPATCH
    public ResponseEntity<Response<Boolean>> toggleAutoRepostGroup(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount ignoredCredentials,
            @RequestParam Long id
    ) throws EntityNotFoundException {
        var response = vkService.toggleAutoRepostGroup(id);

        return ResponseEntity.ok(new Response<>(true, response));
    }

    @VkCredentials
    @GetMapping("/getAllRepostedGroups")
    @SwaggerGetAllRepostedGroupsGET
    public ResponseEntity<Response<List<RepostedGroup>>> getAllRepostedGroups(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) {
        var response = vkService.getAllRepostedGroups(credentials);

        return ResponseEntity.ok(new Response<>(true, response));
    }

    @VkCredentials
    @PostMapping("/addNewRepostedGroup")
    @SwaggerAddNewRepostedGroupPOST
    public ResponseEntity<Response<String>> addNewRepostGroup(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestParam String url
    ) throws ClientException, ApiException, VkNotEnoughRightsException {
        vkService.addNewGroupForRepost(credentials, url);
        return ResponseEntity.ok(new Response<>(true, "success"));
    }

    @VkCredentials
    @Operation(
            summary = "Change likingNewPostsInGroups flag",
            parameters = @Parameter(name = "vkId", schema = @Schema(type = "number"), description = "Id of managed vk account."))
    @PatchMapping("/likingNewPostsInGroups")
    public ResponseEntity<Response<Boolean>> toggleLikingNewPostsInGroups(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) {
        boolean newValue = vkService.toggleLikingNewPostsInGroups(credentials);
        return ResponseEntity.ok(new Response<>(true, newValue));
    }

    @VkCredentials
    @Operation(
            summary = "Change likingNewPhotosOfFriends flag",
            parameters = @Parameter(name = "vkId", schema = @Schema(type = "number"), description = "Id of managed vk account."))
    @PatchMapping("/likingNewPhotosOfFriends")
    public ResponseEntity<Response<Boolean>> toggleLikingNewPhotosOfFriends(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) {
        boolean newValue = vkService.toggleLikingNewPhotosOfFriends(credentials);
        return ResponseEntity.ok(new Response<>(true, newValue));
    }

    @VkCredentials
    @Operation(
            summary = "Change commentingNewPostsInGroups flag",
            parameters = @Parameter(name = "vkId", schema = @Schema(type = "number"), description = "Id of managed vk account."))
    @PatchMapping("/commentingNewPostsInGroups")
    public ResponseEntity<Response<Boolean>> toggleCommentingNewPostsInGroups(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) {
        boolean newValue = vkService.toggleCommentingNewPostsInGroups(credentials);
        return ResponseEntity.ok(new Response<>(true, newValue));
    }

    @VkCredentials
    @Operation(
            summary = "Change likingPostsWhenSubscribeGroup flag",
            parameters = @Parameter(name = "vkId", schema = @Schema(type = "number"), description = "Id of managed vk account."))
    @PatchMapping("/likingPostsWhenSubscribeGroup")
    public ResponseEntity<Response<Boolean>> toggleLikingPostsWhenSubscribeGroup(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) {
        boolean newValue = vkService.toggleLikingPostsWhenSubscribeGroup(credentials);
        return ResponseEntity.ok(new Response<>(true, newValue));
    }

    @VkCredentials
    @Operation(
            summary = "Change likingPhotosWhenApproveFriendship flag",
            parameters = @Parameter(name = "vkId", schema = @Schema(type = "number"), description = "Id of managed vk account."))
    @PatchMapping("/likingPhotosWhenApproveFriendship")
    public ResponseEntity<Response<Boolean>> toggleLikingPhotosWhenApproveFriendship(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) {
        boolean newValue = vkService.toggleLikingPhotosWhenApproveFriendship(credentials);
        return ResponseEntity.ok(new Response<>(true, newValue));
    }

    @VkCredentials
    @Operation(
            summary = "Change repostingNewPostsInGroups flag",
            parameters = @Parameter(name = "vkId", schema = @Schema(type = "number"), description = "Id of managed vk account."))
    @PatchMapping("/repostingNewPostsInGroups")
    public ResponseEntity<Response<Boolean>> setAutoRepost(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) {
        boolean newValue = vkService.toggleRepostingNewPostsInGroups(credentials);
        return ResponseEntity.ok(new Response<>(true, newValue));
    }

    @VkCredentials
    @Operation(
            summary = "Change commentingPostsWhenSubscribeGroup flag",
            parameters = @Parameter(name = "vkId", schema = @Schema(type = "number"), description = "Id of managed vk account."))
    @PatchMapping("/commentingPostsWhenSubscribeGroup")
    public ResponseEntity<Response<Boolean>> setAutoCommentPost(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) {
        boolean newValue = vkService.toggleCommentingPostsWhenSubscribeGroup(credentials);
        return ResponseEntity.ok(new Response<>(true, newValue));
    }

    @VkCredentials
    @Operation(
            summary = "Change commentingPhotosWhenApproveFriendship flag",
            parameters = @Parameter(name = "vkId", schema = @Schema(type = "number"), description = "Id of managed vk account."))
    @PatchMapping("/commentingPhotosWhenApproveFriendship")
    public ResponseEntity<Response<Boolean>> setAutoCommentPhoto(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) {
        boolean newValue = vkService.toggleCommentingPhotosWhenApproveFriendship(credentials);
        return ResponseEntity.ok(new Response<>(true, newValue));
    }

    @VkCredentials
    @Operation(
            summary = "Change approvingFriendship flag",
            parameters = @Parameter(name = "vkId", schema = @Schema(type = "number"), description = "Id of managed vk account."))
    @PatchMapping("/approvingFriendship")
    public ResponseEntity<Response<Boolean>> setAutoApproving(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials
    ) {
        boolean newValue = vkService.toggleApprovingFriendship(credentials);
        return ResponseEntity.ok(new Response<>(true, newValue));
    }

    @VkCredentials
    @SwaggerStoriesEditPATCH
    @PatchMapping("/stories/edit")
    public ResponseEntity<Response<String>> editScheduledStories(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount account,
            @RequestBody StoriesDTO request
    ) throws WrongBodyStructureException, EntityNotFoundException {
        vkService.editStories(account, request);

        return ResponseEntity.ok(new Response<>(true, "success"));
    }

    @VkCredentials
    @GetMapping("/stories/{storiesId}")
    @Operation(
            summary = "Get scheduled stories by id.", tags = {"v1/vk"},
            parameters = @Parameter(name = "vkId", schema = @Schema(type = "number"), description = "Id of managed vk account."))
    public ResponseEntity<Response<StoriesDTO>> getScheduledStoriesById(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount account,
            @PathVariable(name = "storiesId") String storiesId
    ) throws EntityNotFoundException {
        var response = vkService.getScheduledStoriesByIdAndAccount(account, Long.valueOf(storiesId));

        return ResponseEntity.ok(new Response<>(true, response));
    }

    @VkCredentials
    @SwaggerStoriesGET
    @GetMapping("/stories")
    public ResponseEntity<Response<PaginateResponse<StoriesDTO>>> getScheduledStories(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @Parameter(hidden = true) @SortDefault(sort = "publishDate", direction = Sort.Direction.ASC)
            @PageableDefault(size = 25) Pageable pageable
    ) {
        var response = vkService.getScheduledStories(credentials, pageable);

        return ResponseEntity.ok(new Response<>(true, response));
    }

    @VkCredentials
    @SwaggerStoriesUploadPOST
    @PostMapping(value = "/stories/upload", consumes = "multipart/form-data")
    public ResponseEntity<Response<UploadStoriesResultDTO>> storiesUpload(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestParam(value = "media") MultipartFile media
    ) throws ClientException, IOException, WrongContentTypeException, ApiException, WrongBodyStructureException {
        if (FeatureFlags.UPLOAD_STORIES.isActive()) {
            var uploadResponse = vkService.uploadStories(credentials, media);
            return ResponseEntity.ok(new Response<>(true, uploadResponse));
        } else {
            System.out.println("Feature flag is disabled.");
            return null;
        }
    }

    @VkCredentials
    @PostMapping("/stories/save")
    @SwaggerStoriesSavePOST
    public ResponseEntity<Response<List<String>>> saveStories(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestBody List<StoriesDTO> listOfStories
    ) throws ClientException, ApiException {
        var response = vkService.saveStories(credentials, listOfStories);

        return ResponseEntity.ok(new Response<>(true, response));
    }

    @VkCredentials
    @PostMapping(value = "/video/upload", consumes = "multipart/form-data")
    @SwaggerVideoUploadPOST
    public ResponseEntity<Response<String>> videoUpload(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestParam(value = "video") MultipartFile video
    ) throws WrongContentTypeException, ClientException, ApiException, IOException {
        String attachId = vkService.uploadVideo(credentials, video);

        return ResponseEntity.ok(new Response<>(true, attachId));
    }

    @VkCredentials
    @PostMapping(value = "/photos/upload", consumes = "multipart/form-data")
    @SwaggerPhotosUploadPOST
    public ResponseEntity<Response<PhotoUploadResultDTO>> photosUpload(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestParam(value = "image") MultipartFile image
    ) throws WrongContentTypeException, IOException, ClientException, ApiException {
        var resultDTO = vkService.uploadPhotos(credentials, image);

        return ResponseEntity.ok(new Response<>(true, resultDTO));
    }

    @VkCredentials
    @PostMapping("/wall/post")
    @SwaggerWallPostPOST
    public ResponseEntity<Response<String>> wallPost(
            @RequestParam Integer vkId,
            @Parameter(hidden = true) VkAccount credentials,
            @RequestBody(required = false) WallPostRequest request
    ) throws ClientException, ApiException {
        vkService.addNewPostToWall(credentials, request);

        return ResponseEntity.ok(new Response<>(true, "success"));
    }

    @PostMapping("/credentials")
    @Operation(summary = "Save user credentials.", tags = {"v1/vk"})
    public ResponseEntity<Response<String>> saveCredentials(
            @RequestBody CredentialsSaveRequest request,
            @Parameter(hidden = true) @CurrentUser User user
    ) throws ClientException, ApiException {
        vkService.saveCredentials(request, user);

        return ResponseEntity.ok(new Response<>(true, "Success!"));
    }
}

