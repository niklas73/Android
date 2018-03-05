/*
 * TrashOut is an environmental project that teaches people how to recycle 
 * and showcases the worst way of handling waste - illegal dumping. All you need is a smart phone.
 *  
 *  
 * There are 10 types of programmers - those who are helping TrashOut and those who are not.
 * Clean up our code, so we can clean up our planet. 
 * Get in touch with us: help@trashout.ngo
 *  
 * Copyright 2017 TrashOut, n.f.
 *  
 * This file is part of the TrashOut project.
 *  
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * See the GNU General Public License for more details: <https://www.gnu.org/licenses/>.
 */

package me.trashout.service;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import me.trashout.Configuration;
import me.trashout.api.base.ApiBaseDataResult;
import me.trashout.api.base.ApiBaseRequest;
import me.trashout.api.base.ApiSimpleErrorResult;
import me.trashout.api.base.ApiSimpleResult;
import me.trashout.api.request.ApiUpdateTrashRequest;
import me.trashout.model.Image;
import me.trashout.model.Trash;
import me.trashout.service.base.BaseService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateTrashService extends BaseService {

    private static List<ApiBaseRequest> mRequestList = new ArrayList<>();

    public static void startForRequest(Context context, int requestId, long trashId, Trash trash, ArrayList<Uri> imagesUri) {
        ApiUpdateTrashRequest apiUpdateTrashRequest = new ApiUpdateTrashRequest(requestId, trashId, trash, imagesUri);
        addRequest(context, UpdateTrashService.class, apiUpdateTrashRequest, mRequestList);
    }

    @Override
    protected List<ApiBaseRequest> getRequestList() {
        return mRequestList;
    }

    @Override
    protected void requestProcess(final ApiBaseRequest apiBaseRequest) {
        apiBaseRequest.setStatus(ApiBaseRequest.Status.PENDING);

        final ApiUpdateTrashRequest apiUpdateTrashRequest = (ApiUpdateTrashRequest) apiBaseRequest;

        StorageReference mStorageRef = FirebaseStorage.getInstance().getReference(Configuration.FIREBASE_IMAGE_FOLDER_REFERENCE);

        final ArrayList<Image> trashImages = new ArrayList<>(apiUpdateTrashRequest.getImagesUri().size());
        uploadImages(mStorageRef, apiUpdateTrashRequest.getImagesUri(), 0, trashImages, new OnImageUploadListener() {
            @Override
            public void onComplete(UploadStatus status, ArrayList<Image> images) {
                if (status.equals(UploadStatus.SUCCESS)) {

                    Trash trash = apiUpdateTrashRequest.getTrash();
                    trash.setImages(images);

                    Call<ResponseBody> call = mApiServer.updateTrash(apiUpdateTrashRequest.getTrashId(), trash);
                    call.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {


                            ApiBaseDataResult result = null;
                            if (response.isSuccessful() && response.body() != null) {
                                Log.d(TAG, "onResponse: isSuccessful");

                                result = new ApiSimpleResult();
                            } else {
                                Log.d(TAG, "onResponse: fail");
                                result = new ApiSimpleErrorResult(getBaseContext());
                            }


                            apiBaseRequest.setStatus(ApiBaseRequest.Status.DONE);
                            notifyResultListener(apiBaseRequest.getId(), apiBaseRequest, result, response, null);
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable retrofitError) {
                            Log.d(TAG, "RequestProcess - FAIL \n" + retrofitError.toString());
                            apiBaseRequest.setStatus(ApiBaseRequest.Status.ERROR);
                            notifyResultListener(apiBaseRequest.getId(), null, null, retrofitError);
                        }
                    });
                } else {
                    apiBaseRequest.setStatus(ApiBaseRequest.Status.DONE);
                    notifyResultListener(apiBaseRequest.getId(), null, null, null);
                }
            }
        });
    }
}
