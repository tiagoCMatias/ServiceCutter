package ch.hsr.servicestoolkit.editor.web.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.codahale.metrics.annotation.Timed;

import ch.hsr.servicestoolkit.editor.security.AuthoritiesConstants;

@RestController
@RequestMapping("/api/editor")
@Secured(AuthoritiesConstants.USER)
public class EditorResource {

	private final Logger log = LoggerFactory.getLogger(EditorResource.class);
	private final RestTemplate rest = new RestTemplate();
	@Value("http://${application.links.engine.host}:${application.links.engine.port}")
	private String engineUrl;

	@RequestMapping(value = "/model", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	public ResponseEntity<?> uploadModelFile(@RequestParam("file") final MultipartFile file) {
		ResponseEntity<?> result = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		try {
			String theString = IOUtils.toString(file.getInputStream());
			log.trace("file content:{}", theString);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<?> requestEntity = new HttpEntity<Object>(theString, headers);
			@SuppressWarnings("rawtypes")
			ResponseEntity<Map> responseEntity = rest.exchange(engineUrl + "/engine/import", HttpMethod.POST, requestEntity, Map.class);
			@SuppressWarnings("unchecked")
			Map<String, Object> serviceResponse = responseEntity.getBody();
			serviceResponse.put("message", "Upload successful!");
			log.debug("importer response: {}", serviceResponse);
			result = new ResponseEntity<>(serviceResponse, HttpStatus.CREATED);
		} catch (HttpClientErrorException e) {
			log.error("", e.getResponseBodyAsString());
			Map<String, Object> serviceResponse = new HashMap<>();
			serviceResponse.put("message", "Upload failed!");
			serviceResponse.put("jsonError", e.getResponseBodyAsString());
			result = new ResponseEntity<>(serviceResponse, HttpStatus.OK);
		} catch (IOException e) {
			log.error("", e);
		}
		return result;
	}

	@RequestMapping(value = "/model/{modelId}/userrepresentations", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	public ResponseEntity<?> uploadUserRepresentationsFile(@RequestParam("file") final MultipartFile file, @PathVariable("modelId") final String modelId) {
		ResponseEntity<?> result = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		Map<String, Object> resultBody = new HashMap<>();
		try {
			String theString = IOUtils.toString(file.getInputStream());
			log.debug("modelId:{}", modelId);
			log.debug("file content:{}", theString);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<?> requestEntity = new HttpEntity<Object>(theString, headers);
			String path = engineUrl + "/engine/import/" + modelId + "/" + "userrepresentations" + "/";
			log.debug("post on {}", path);
			rest.exchange(path, HttpMethod.POST, requestEntity, Void.class);
			resultBody.put("message", "Upload successfull!");
			result = ResponseEntity.ok(resultBody);
		} catch (HttpClientErrorException e) {
			log.error("", e.getResponseBodyAsString());
			resultBody.put("message", "Upload failed");
			resultBody.put("jsonError", e.getResponseBodyAsString());
			result = ResponseEntity.ok(resultBody);
		} catch (IOException e) {
			log.error("", e);
		}
		return result;
	}

}
