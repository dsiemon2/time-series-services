package gov.usgs.wma.waterdata.collections;

import static gov.usgs.wma.waterdata.collections.CollectionParams.PARAM_COLLECTION_ID;
import static gov.usgs.wma.waterdata.collections.CollectionParams.PARAM_FEATURE_ID;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Min;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gov.usgs.wma.waterdata.OgcException;
import gov.usgs.wma.waterdata.openapi.schema.collections.CollectionGeoJSON;
import gov.usgs.wma.waterdata.openapi.schema.collections.CollectionsGeoJSON;
import gov.usgs.wma.waterdata.openapi.schema.collections.FeatureCollectionGeoJSON;
import gov.usgs.wma.waterdata.openapi.schema.collections.FeatureGeoJSON;
import gov.usgs.wma.waterdata.parameter.BoundingBox;
import gov.usgs.wma.waterdata.validation.BBox;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Observations - OGC api", description = "Feature Collections")
@RestController
@Validated
public class CollectionsController extends BaseController {
	protected CollectionsDao collectionsDao;

	protected CollectionParams collectionsParams;

	protected static final String LIMIT_VALIDATE_MESS = "limit must be greater than or equal to 1";
	protected static final String START_INDEX_VALIDATE_MESS = "startIndex must be greater than or equal to 0";
	protected static final String BBOX_DESCRIPTION = "Bounding box: minimum longitude, minimum latitude, maximum longitude, maximum latitude<br>"
			+ "bbox=-109.046667,37.0,-102.046667,39.0 limits results to monitoring sites in Colorado.";

	@Autowired
	public CollectionsController(CollectionsDao collectionsDao, CollectionParams collectionsParams) {
		this.collectionsDao = collectionsDao;
		this.collectionsParams = collectionsParams;
	}

	@Operation(
			description = "Return GeoJSON representation of the Collections.",
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "GeoJSON representation of the Collections.",
							content = @Content(schema = @Schema(implementation = CollectionsGeoJSON.class))),
					@ApiResponse(
							responseCode = "500",
							description = HTTP_500_DESCRIPTION,
							content = @Content(schema = @Schema(implementation = OgcException.class)))
			},
			externalDocs=@ExternalDocumentation(url="http://docs.opengeospatial.org/is/17-069r3/17-069r3.html#_collections_")
		)
	@GetMapping(value = "collections", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getOgcCollections(@RequestParam(value = "f", required = false, defaultValue = "json") String mimeType,
			HttpServletResponse response) {

		return collectionsDao.getCollectionsJson(collectionsParams.buildParams(null));
	}

	@Operation(
			description = "Return GeoJSON Data specific to the requested Collection.",
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "GeoJSON representation of the Collection.",
							content = @Content(schema = @Schema(implementation = CollectionGeoJSON.class))),
					@ApiResponse(
							responseCode = "404",
							description = HTTP_404_DESCRIPTION,
							content = @Content(schema = @Schema(implementation = OgcException.class))),
					@ApiResponse(
							responseCode = "500",
							description = HTTP_500_DESCRIPTION,
							content = @Content(schema = @Schema(implementation = OgcException.class)))
			},
			externalDocs=@ExternalDocumentation(url="http://docs.opengeospatial.org/is/17-069r3/17-069r3.html#_collection_")
		)
	@GetMapping(value = "collections/{collectionId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getOgcCollection(@RequestParam(value = "f", required = false, defaultValue = "json") String mimeType,
			@PathVariable(value = PARAM_COLLECTION_ID) String collectionId, HttpServletResponse response) {

		String rtn = collectionsDao.getCollectionJson(collectionsParams.buildParams(collectionId));
		if (rtn == null) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			rtn = ogc404Payload;
		}

		return rtn;
	}

	@Operation(
			description = "Return GeoJSON Data specific to the features in the requested Collection.",
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "GeoJSON representation of the Feature Collection.",
							content = @Content(schema = @Schema(implementation = FeatureCollectionGeoJSON.class))),
					@ApiResponse(
							responseCode = "400",
							description = HTTP_400_DESCRIPTION,
							content = @Content(schema = @Schema(implementation = OgcException.class))),
					@ApiResponse(
							responseCode = "404",
							description = HTTP_404_DESCRIPTION,
							content = @Content(schema = @Schema(implementation = OgcException.class))),
					@ApiResponse(
							responseCode = "500",
							description = HTTP_500_DESCRIPTION,
							content = @Content(schema = @Schema(implementation = OgcException.class)))
			},
			externalDocs=@ExternalDocumentation(url="http://docs.opengeospatial.org/is/17-069r3/17-069r3.html#_feature_")
		)
	@Parameter(name = "bbox", description = BBOX_DESCRIPTION, schema = @Schema(implementation = String.class, type = "string"))
	@GetMapping(value = "collections/{collectionId}/items", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getOgcCollectionFeatures(
			@RequestParam(value = "f", required = false, defaultValue = "json") String mimeType,
			@Min(value=1, message = LIMIT_VALIDATE_MESS) @RequestParam(value = "limit", required = false, defaultValue = "10000") int limit,
			@Min(value=0, message = START_INDEX_VALIDATE_MESS) @RequestParam(value = "startIndex", required = false, defaultValue = "0") int startIndex,
			@BBox @RequestParam(value = "bbox", required = false) BoundingBox bbox,
			@PathVariable(value = PARAM_COLLECTION_ID) String collectionId, HttpServletResponse response) {

		int count = collectionsDao.getCollectionFeatureCount(collectionsParams.buildParams(collectionId));

		String rtn;
		if (startIndex >= count) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			rtn = ogc404Payload;
		} else {
			rtn = collectionsDao.getCollectionFeaturesJson(
					collectionsParams.buildParams(collectionId, limit, startIndex, bbox, count));
			if (rtn == null) {
				response.setStatus(HttpStatus.NOT_FOUND.value());
				rtn = ogc404Payload;
			}
		}

		return rtn;
	}

	@Operation(
			description = "Return GeoJSON Data specific to the requested Collection Feature.",
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "GeoJSON representation of the Feature.",
							content = @Content(schema = @Schema(implementation = FeatureGeoJSON.class))),
					@ApiResponse(
							responseCode = "404",
							description = HTTP_404_DESCRIPTION,
							content = @Content(schema = @Schema(implementation = OgcException.class))),
					@ApiResponse(
							responseCode = "500",
							description = HTTP_500_DESCRIPTION,
							content = @Content(schema = @Schema(implementation = OgcException.class)))
			},
			externalDocs=@ExternalDocumentation(url="http://docs.opengeospatial.org/is/17-069r3/17-069r3.html#_feature_")
		)
	@GetMapping(value = "collections/{collectionId}/items/{featureId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getOgcCollectionFeature(
			@RequestParam(value = "f", required = false, defaultValue = "json") String mimeType,
			@PathVariable(value = PARAM_COLLECTION_ID) String collectionId,
			@PathVariable(value = PARAM_FEATURE_ID) String featureId, HttpServletResponse response) {

		String rtn =collectionsDao.getCollectionFeatureJson(collectionsParams.buildParams(collectionId, featureId));
			if (rtn == null) {
				response.setStatus(HttpStatus.NOT_FOUND.value());
				rtn = ogc404Payload;
			}

		return rtn;
	}
	
}
