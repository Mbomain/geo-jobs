package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.RoadContinuationRequested;
import app.bpartners.geojobs.file.bucket.BucketComponent;
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
import org.springframework.web.multipart.MultipartFile;

public class RoadContinuerServiceIT extends FacadeIT {

  @MockBean private BucketComponent bucketComponent;
  @MockBean private EventProducer<RoadContinuationRequested> eventProducer;
  @MockBean private GeoJsonRoadContinuationRepository repository;
  @Autowired private RoadContinuerService subject;

  public static MultipartFile convertFileToMultipartFile(File file) throws IOException {
    try (FileInputStream fis = new FileInputStream(file)) {
      return new MockMultipartFile("geojson-file", file.getName(), "application/geo+json", fis);
    }
  }

  @Test
  void test_continuation_with_ambohijatovo_geojson_content() throws Exception {
    var resourceUrl = getClass().getResource("/geojson/ambohijatovo-crossed.geojson");
    assertNotNull(resourceUrl);

    var geoJSONMultipartFile = convertFileToMultipartFile(new File(resourceUrl.toURI()));
    var mockedURL = URI.create("https://mocked/ambohijatovo-continued.geojson").toURL();

    var fakeContinuation = new GeoJsonRoadContinuation();
    fakeContinuation.setBucketKey("ambohijatovo-continued.geojson");

    when(bucketComponent.presign(anyString(), any(Duration.class))).thenReturn(mockedURL);
    when(repository.findById(anyString()))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(fakeContinuation));

    var result = subject.makeContinuation(geoJSONMultipartFile, 17, 1024);

    assertNotNull(result);
    assertEquals(mockedURL.toString(), result.get("url"));

    verify(eventProducer).accept(any());
    verify(bucketComponent).presign(anyString(), any(Duration.class));
  }
}
