package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.RoadContinuationRequested;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.file.Hasher;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.repository.GeoJsonRoadContinuationRepository;
import java.io.File;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class RoadContinuationRequestedServiceIT extends FacadeIT {
  private static final TilingConf TILING_CONF = new TilingConf(17, 1_024);
  @Autowired private RoadContinuationRequestedService subject;
  @Autowired private GeoJsonRoadContinuationRepository repository;
  @Autowired private Hasher hasher;
  @MockBean private BucketComponent bucketComponent;

  @Test
  void should_process_road_continuation_and_have_the_correct_file_cred() throws URISyntaxException {
    var resource = getClass().getResource("/geojson/ambohimanjaka.geojson");
    assertNotNull(resource);
    var geoJSON = new File(resource.toURI());

    var expectedHash = hasher.apply(geoJSON).value();
    var event = new RoadContinuationRequested("dummyBucketKey", expectedHash, TILING_CONF);

    when(bucketComponent.download(anyString())).thenReturn(geoJSON);
    when(bucketComponent.upload(any(File.class), anyString())).thenReturn(mock(FileHash.class));
    subject.accept(event);

    var instance = repository.findById(expectedHash);
    assertTrue(instance.isPresent());
    assertNotNull(instance.get().getBucketKey());
  }

  @Test
  void should_process_road_continuation_with_anosy_rond_point() throws URISyntaxException {
    var resource = getClass().getResource("/geojson/anosy-rond-point.geojson");
    assertNotNull(resource);
    var geoJSON = new File(resource.toURI());
    var expectedHash = hasher.apply(geoJSON).value();

    var event = new RoadContinuationRequested("dummyBucketKey", expectedHash, TILING_CONF);

    when(bucketComponent.download(anyString())).thenReturn(geoJSON);
    when(bucketComponent.upload(any(File.class), anyString())).thenReturn(mock(FileHash.class));
    subject.accept(event);

    var instance = repository.findById(expectedHash);
    assertTrue(instance.isPresent());
    assertNotNull(instance.get().getBucketKey());
  }
}
