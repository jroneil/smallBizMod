package com.oneilsoftware.sbp.module.document.internal.web;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.oneilsoftware.sbp.module.document.api.CreateDocument;
import com.oneilsoftware.sbp.module.document.api.Document;
import com.oneilsoftware.sbp.module.document.api.DocumentAuditEvent;
import com.oneilsoftware.sbp.module.document.api.DocumentContent;
import com.oneilsoftware.sbp.module.document.api.DocumentFilter;
import com.oneilsoftware.sbp.module.document.api.DocumentOperations;
import com.oneilsoftware.sbp.module.document.api.DocumentOwnerType;
import com.oneilsoftware.sbp.module.document.api.DocumentPage;
import com.oneilsoftware.sbp.module.document.api.DocumentSizeLimitExceededException;
import com.oneilsoftware.sbp.module.document.api.ReplaceDocumentContent;
import com.oneilsoftware.sbp.module.document.api.UnknownDocumentException;
import com.oneilsoftware.sbp.module.document.api.UnknownDocumentOwnerException;
import com.oneilsoftware.sbp.module.document.api.UpdateDocumentMetadata;
import com.oneilsoftware.sbp.module.document.internal.DocumentProperties;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST surface of the Document capability. Uploads are multipart ({@code file} part plus
 * {@code ownerType}, {@code ownerId}, and optional {@code displayName} text parts); downloads are
 * always served with attachment disposition so the browser saves the file rather than rendering
 * stored content inline (Section 13 of the supplement).
 */
@RestController
public class DocumentController {

	private final DocumentOperations documentOperations;
	private final DocumentProperties properties;

	public DocumentController(DocumentOperations documentOperations, DocumentProperties properties) {
		this.documentOperations = documentOperations;
		this.properties = properties;
	}

	@PostMapping(value = "/api/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Document> create(
			@RequestPart("file") MultipartFile file,
			@RequestParam("ownerType") DocumentOwnerType ownerType,
			@RequestParam("ownerId") UUID ownerId,
			@RequestParam(value = "displayName", required = false) String displayName) throws IOException {
		rejectOverLimit(file);
		String name = (displayName == null || displayName.isBlank())
				? file.getOriginalFilename()
				: displayName;
		Document document = documentOperations.create(new CreateDocument(
				ownerType,
				ownerId,
				name,
				file.getContentType(),
				file.getBytes()));
		return ResponseEntity.created(URI.create("/api/documents/" + document.id())).body(document);
	}

	@GetMapping("/api/documents")
	public DocumentPage list(
			@RequestParam(required = false) String query,
			@RequestParam(required = false) DocumentOwnerType ownerType,
			@RequestParam(required = false) UUID ownerId,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		return documentOperations.search(new DocumentFilter(
				query,
				ownerType,
				ownerId,
				page == null ? 0 : page,
				size == null ? DocumentFilter.DEFAULT_PAGE_SIZE : size));
	}

	@GetMapping("/api/documents/{documentId}")
	public ResponseEntity<Document> get(@PathVariable UUID documentId) {
		return ResponseEntity.of(documentOperations.findById(documentId));
	}

	@PutMapping("/api/documents/{documentId}")
	public Document updateDisplayName(
			@PathVariable UUID documentId,
			@Valid @RequestBody UpdateDocumentMetadataRequest request) {
		return documentOperations.updateDisplayName(
				documentId,
				new UpdateDocumentMetadata(request.displayName()));
	}

	@PutMapping(value = "/api/documents/{documentId}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Document replaceContent(
			@PathVariable UUID documentId,
			@RequestPart("file") MultipartFile file) throws IOException {
		rejectOverLimit(file);
		return documentOperations.replaceContent(
				documentId,
				new ReplaceDocumentContent(file.getContentType(), file.getBytes()));
	}

	@DeleteMapping("/api/documents/{documentId}")
	public ResponseEntity<Void> delete(@PathVariable UUID documentId) {
		documentOperations.delete(documentId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/api/documents/{documentId}/content")
	public ResponseEntity<byte[]> download(@PathVariable UUID documentId) {
		DocumentContent content = documentOperations.download(documentId);
		return ResponseEntity.ok()
				.contentType(safeContentType(content.contentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(content.displayName()).build().toString())
				.body(content.content());
	}

	@GetMapping("/api/documents/{documentId}/audit-events")
	public List<DocumentAuditEvent> auditEvents(
			@PathVariable UUID documentId,
			@RequestParam(required = false) String action) {
		return documentOperations.listAuditEvents(documentId, action);
	}

	/**
	 * Rejects files over the configured limit before they are read into memory. The application
	 * service enforces the same limit authoritatively for every caller (NFR-DOC-07).
	 */
	private void rejectOverLimit(MultipartFile file) {
		if (file.getSize() > properties.maxSizeBytes()) {
			throw new DocumentSizeLimitExceededException(properties.maxSizeBytes(), file.getSize());
		}
	}

	private static MediaType safeContentType(String contentType) {
		try {
			return MediaType.parseMediaType(contentType);
		} catch (IllegalArgumentException e) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	@ExceptionHandler({UnknownDocumentException.class, UnknownDocumentOwnerException.class})
	ResponseEntity<Void> unknownDocumentOrOwner() {
		return ResponseEntity.notFound().build();
	}

	@ExceptionHandler(DocumentSizeLimitExceededException.class)
	ResponseEntity<Void> sizeLimitExceeded() {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<Void> multipartSizeLimitExceeded() {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<Void> invalidRequest() {
		return ResponseEntity.badRequest().build();
	}
}
