package com.projectversionone.awsimageupload.profile;

import com.projectversionone.awsimageupload.bucket.BucketName;
import com.projectversionone.awsimageupload.filestore.FileStore;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class UserProfileService {
    private final UserProfileDataAcessService userProfileDataAcessService;
    private final FileStore fileStore;

    @Autowired
    public UserProfileService (
            UserProfileDataAcessService userProfileDataAcessService,
                               FileStore fileStore) {
        this.userProfileDataAcessService = userProfileDataAcessService;
        this.fileStore = fileStore;
    }

    List<UserProfile> getUserProfiles(){
        return userProfileDataAcessService.getUserProfiles();
    }

    public void uploadUserProfileImage(UUID userProfileID, MultipartFile file) {
        isFileEmpty(file);

        isImage(file);

        UserProfile user = getUserProfileOrThrow(userProfileID);

        Map<String, String> metaData = extractMetadata(file);

        String path = String.format("%s/%s", BucketName.PROFILE_IMAGE.getBucketName(), user.getUserProfileID());
        String filename = String.format("%s-%s",file.getOriginalFilename(), UUID.randomUUID());

        try{
            fileStore.save(path,filename,Optional.of(metaData),file.getInputStream());
            user.setProfileImageLink(filename);
        }catch(IOException e){
           throw new IllegalStateException(e);
        }



    }
    private static Map<String, String> extractMetadata(MultipartFile file) {
        Map<String,String> metaData = new HashMap<>();
        metaData.put("Content-Type", file.getContentType());
        metaData.put("Content-Length",String.valueOf(file.getSize()));
        return metaData;
    }

    private UserProfile getUserProfileOrThrow(UUID userProfileID) {
        UserProfile user = userProfileDataAcessService
                .getUserProfiles()
                .stream()
                .filter(userProfile -> userProfile.getUserProfileID().equals(userProfileID))
                .findFirst()
                .orElseThrow(()-> new IllegalStateException(String.format("User profile %s not found.", userProfileID)));
        return user;
    }

    private static void isImage(MultipartFile file) {
        if(!Arrays.asList(ContentType.IMAGE_JPEG.getMimeType(), ContentType.IMAGE_GIF.getMimeType(), ContentType.IMAGE_PNG.getMimeType()).contains(file.getContentType())){
            throw new IllegalStateException("Does not contain correct file_type. File must be an image. " + file.getContentType());
        }
    }

    private static void isFileEmpty(MultipartFile file) {
        if(file.isEmpty()){
            throw new IllegalStateException("Cannot upload file " + file.getSize());
        }
    }
}
