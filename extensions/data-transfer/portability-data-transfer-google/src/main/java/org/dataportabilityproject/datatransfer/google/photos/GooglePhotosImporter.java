/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.datatransfer.google.photos;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.annotations.VisibleForTesting;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaStreamSource;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.ImportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.types.TempPhotosData;
import org.dataportabilityproject.transfer.ImageStreamProvider;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GooglePhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  static final String ALBUM_POST_URL = "https://picasaweb.google.com/data/feed/api/user/default";
  static final String PHOTO_POST_URL_FORMATTER =
      "https://picasaweb.google.com/data/feed/api/user/default/albumid/%s";
  // The default album to upload to if the photo is not associated with an album
  static final String DEFAULT_ALBUM_ID = "default";
  static final Logger logger = LoggerFactory.getLogger(GooglePhotosImporter.class);

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private volatile PicasawebService photosService;
  private final ImageStreamProvider imageStreamProvider;

  public GooglePhotosImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore) {
    this(credentialFactory, jobStore, null, new ImageStreamProvider());
  }

  @VisibleForTesting
  GooglePhotosImporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore,
      PicasawebService photosService,
      ImageStreamProvider imageStreamProvider) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.photosService = photosService;
    this.imageStreamProvider = imageStreamProvider;
  }

  @Override
  public ImportResult importItem(
      UUID jobId, TokensAndUrlAuthData authData, PhotosContainerResource data) {
    if (data.getAlbums() != null && data.getAlbums().size() > 0) {
      logger.warn(
          "Importing albums in Google Photos is not supported. "
              + "Photos will be added to the default album.");
    }

    try {
      for (PhotoModel photo : data.getPhotos()) {
        importSinglePhoto(authData, photo);
      }
    } catch (IOException | ServiceException e) {
      // TODO(olsona): we shouldn't just error out if there's a single problem - should retry
      return new ImportResult(ResultType.ERROR, e.getMessage());
    }

    return ImportResult.OK;
  }

  @VisibleForTesting
  void importSinglePhoto(TokensAndUrlAuthData authData, PhotoModel inputPhoto)
      throws IOException, ServiceException {

    // Set up photo
    PhotoEntry outputPhoto = new PhotoEntry();
    outputPhoto.setTitle(new PlainTextConstruct("copy of " + inputPhoto.getTitle()));
    outputPhoto.setDescription(new PlainTextConstruct(inputPhoto.getDescription()));
    outputPhoto.setClient(GoogleStaticObjects.APP_NAME);

    String mediaType = inputPhoto.getMediaType();
    if (mediaType == null) {
      mediaType = "image/jpeg";
    }

    MediaStreamSource streamSource =
        new MediaStreamSource(imageStreamProvider.get(inputPhoto.getFetchableUrl()), mediaType);
    outputPhoto.setMediaSource(streamSource);

    String albumId = DEFAULT_ALBUM_ID;
    URL uploadUrl = new URL(String.format(PHOTO_POST_URL_FORMATTER, albumId));

    // Upload photo
    getOrCreatePhotosService(authData).insert(uploadUrl, outputPhoto);
  }

  private PicasawebService getOrCreatePhotosService(TokensAndUrlAuthData authData) {
    return photosService == null ? makePhotosService(authData) : photosService;
  }

  private synchronized PicasawebService makePhotosService(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    PicasawebService service = new PicasawebService(GoogleStaticObjects.APP_NAME);
    service.setOAuth2Credentials(credential);
    return service;
  }
}
