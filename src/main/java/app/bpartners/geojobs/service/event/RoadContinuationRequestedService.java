package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.lang.Math.PI;

import app.bpartners.geojobs.endpoint.event.model.RoadContinuationRequested;
import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.LatLonLinesContinuer;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.RoutesContinuationConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import app.bpartners.geojobs.repository.GeoJsonRoadContinuationRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonRoadContinuation;
import jakarta.ws.rs.ProcessingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class RoadContinuationRequestedService implements Consumer<RoadContinuationRequested> {
  private static final AlphaConf DEFAULT_ALPHA_CONF = new AlphaConf(0.5d, 1);
  private static final UnionConf DEFAULT_UNION_CONF = new UnionConf(1);
  private static final PrettyConf DEFAULT_PRETTY_CONF = new PrettyConf(0);
  private static final int DEFAULT_NEIGHBOUR_THRESHOLD = 10;
  private static final ContinuationConf DEFAULT_CONTINUATION_CONF =
      new ContinuationConf(PI / 12, PI / 6, 500);

  private final BucketComponent bucketComponent;
  private final GeoJsonRoadContinuationRepository continuationRepository;

  private static File getGeoJsonFromString(String geoJsonString) {
    String uuidName = UUID.randomUUID().toString();
    File tempFile;
    try {
      tempFile = File.createTempFile("continued-geojson-" + uuidName, ".geojson");
      Files.writeString(tempFile.toPath(), geoJsonString);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return tempFile;
  }

  private static LatLonLinesContinuer getLatLonContinuer(
      RoutesContinuationConf routesContinuationConf, TilingConf tilingConf) {
    return new LatLonLinesContinuer(
        routesContinuationConf, tilingConf, DEFAULT_NEIGHBOUR_THRESHOLD);
  }

  private static RoutesContinuationConf getRouteContinuationConf() {
    return new RoutesContinuationConf(
        DEFAULT_ALPHA_CONF, DEFAULT_UNION_CONF, DEFAULT_CONTINUATION_CONF, DEFAULT_PRETTY_CONF);
  }

  @Override
  public void accept(RoadContinuationRequested continuationRequested) {
    File geoJsonFile = bucketComponent.download(continuationRequested.getBucketKey());
    String hash = continuationRequested.getHash();

    log.info(
        "RoadContinuationRequested received, asynchronous road continuation process started"
            + ": id={}",
        hash);

    GeoJsonRoadContinuation record = new GeoJsonRoadContinuation(hash, null, PROCESSING);

    continuationRepository.save(record);

    var continuedGeoJsonFile = makeContinuation(geoJsonFile, continuationRequested.getTilingConf());

    var bucketKey = "road-continuation/continued/" + UUID.randomUUID() + ".geojson";
    var fileHash = bucketComponent.upload(continuedGeoJsonFile, bucketKey);

    if (fileHash == null)
      throw new ProcessingException("Could not upload the geojson with continued road");

    log.info("Road continuation done : id={}, bucket_key={}", fileHash.value(), bucketKey);

    record.setBucketKey(bucketKey);
    record.setStatus(FINISHED);
    continuationRepository.save(record);
  }

  public File makeContinuation(File geoJSON, TilingConf tilingConf) {
    log.info(
        "Continuing route polygons of geojson={} with zoom={} and imgSize={}",
        geoJSON.getName(),
        tilingConf.z(),
        tilingConf.imgSize());
    var continuer = getLatLonContinuer(getRouteContinuationConf(), tilingConf);
    var continuedPolygons = continuer.apply(geoJSON);

    var continuedGeoJsonFile = getGeoJsonFromString(new Geojson(continuedPolygons).stringValue());
    log.info("Continuation process finished");
    return continuedGeoJsonFile;
  }
}
