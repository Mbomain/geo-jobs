package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.RoadContinuationRequested;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.GeoJsonRoadContinuationRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonRoadContinuation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

class RoadContinuerControllerIT extends FacadeIT {
  @Autowired private RoadContinuerController subject;
  @MockBean private BucketComponent bucketComponent;
  @MockBean private EventProducer<RoadContinuationRequested> eventProducer;
  @MockBean private GeoJsonRoadContinuationRepository repository;

  public static MockMultipartFile convertFileToMultipartFile(File file) throws IOException {
    try (FileInputStream fis = new FileInputStream(file)) {
      return new MockMultipartFile("geojson-file", file.getName(), "application/geo+json", fis);
    }
  }

  @Test
  void test_continuation_of_quai_de_bourbon() throws Exception {
    var resource = getClass().getResource("/geojson/quai-de-bourbon.geojson");
    assertNotNull(resource);

    final String preSignedUrl = "https://mocked-s3-url.com/quai-de-bourbon-continued-roads.geojson";
    var uploadedGeoJSON = convertFileToMultipartFile(new File(resource.toURI()));

    var fakeContinuation = new GeoJsonRoadContinuation();
    fakeContinuation.setBucketKey("quai-de-bourbon-continued-roads.geojson");

    when(bucketComponent.upload(any(File.class), anyString())).thenReturn(mock(FileHash.class));
    when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenReturn(URI.create(preSignedUrl).toURL());
    when(repository.findById(anyString()))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(fakeContinuation));

    var actual = subject.roadContinuer(uploadedGeoJSON, 17, 1024);

    assertNotNull(actual);
    assertEquals(preSignedUrl, actual.url());

    verify(eventProducer).accept(any());
    verify(bucketComponent).presign(anyString(), any(Duration.class));
  }

  @Test
  void test_continuation_of_non_valid_file() throws Exception {
    var resource = getClass().getResource("/shape/dummy.shape");
    assertNotNull(resource);

    var uploadedGeoJSON = convertFileToMultipartFile(new File(resource.toURI()));
    assertNotNull(uploadedGeoJSON);

    assertThrows(
        BadRequestException.class, () -> subject.roadContinuer(uploadedGeoJSON, 20, 1_024));
  }
}
