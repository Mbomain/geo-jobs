package app.bpartners.geojobs.endpoint.rest.controller;

import app.bpartners.geojobs.endpoint.model.RoadContinuationResponse;
import app.bpartners.geojobs.endpoint.rest.validator.GeoJsonValidator;
import app.bpartners.geojobs.service.RoadContinuerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@RestController
public class RoadContinuerController {

  private RoadContinuerService roadContinuerService;
  private GeoJsonValidator geoJsonValidator;

  @PostMapping("/road-continuer")
  public RoadContinuationResponse roadContinuer(
      @RequestParam("geojson-file") MultipartFile geoJson,
      @RequestParam Integer zoom,
      @RequestParam Integer imageSize) {
    geoJsonValidator.accept(geoJson);
    return roadContinuerService.makeContinuation(geoJson, zoom, imageSize);
  }
}
