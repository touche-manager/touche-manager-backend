package com.touchemanager.athlete.service.impl;

import com.touchemanager.athlete.dto.AthleteDocumentResponse;
import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.entity.AthleteDocument;
import com.touchemanager.athlete.entity.DocumentType;
import com.touchemanager.athlete.repository.AthleteDocumentRepository;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.shared.service.FileStorageService;
import com.touchemanager.auth.entity.Usuario;
import com.touchemanager.auth.repository.UsuarioRepository;
import com.touchemanager.shared.exception.AthleteNotFoundException;
import com.touchemanager.shared.exception.DocumentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AthleteDocumentServiceImplTest {

    @Mock
    private AthleteDocumentRepository athleteDocumentRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AthleteDocumentServiceImpl athleteDocumentService;

    private Usuario usuario;
    private Athlete athlete;
    private AthleteDocument document;
    private String email;
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        email = "athlete@test.com";

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail(email);

        athlete = new Athlete();
        athlete.setId(10L);
        athlete.setUser(usuario);

        document = new AthleteDocument();
        document.setId(100L);
        document.setAthlete(athlete);
        document.setFileKey("athletes/10/medical_clearance/key.pdf");
        document.setContentType("application/pdf");
        document.setDocumentType(DocumentType.MEDICAL_CLEARANCE);
        document.setDescription("Test Desc");
        document.setUploadDate(LocalDateTime.now());

        mockFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test data".getBytes()
        );
    }

    @Test
    void uploadDocument_Success() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(athleteRepository.findByUserId(usuario.getId())).thenReturn(Optional.of(athlete));
        when(fileStorageService.uploadFile(any(), anyString())).thenReturn("athletes/10/medical_clearance/key.pdf");
        when(athleteDocumentRepository.save(any(AthleteDocument.class))).thenReturn(document);

        AthleteDocumentResponse response = athleteDocumentService.uploadDocument(
                email, mockFile, DocumentType.MEDICAL_CLEARANCE, "Test Desc");

        assertNotNull(response);
        assertEquals(document.getId(), response.id());
        assertEquals(document.getDocumentType(), response.documentType());

        verify(usuarioRepository, times(1)).findByEmail(email);
        verify(athleteRepository, times(1)).findByUserId(usuario.getId());
        verify(fileStorageService, times(1)).uploadFile(any(), anyString());
        verify(athleteDocumentRepository, times(1)).save(any(AthleteDocument.class));
    }

    @Test
    void uploadDocument_WithDescription_RenamesFileCorrectly() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(athleteRepository.findByUserId(usuario.getId())).thenReturn(Optional.of(athlete));
        when(fileStorageService.uploadFile(any(), anyString())).thenReturn("athletes/10/medical_clearance/key.pdf");
        when(athleteDocumentRepository.save(any(AthleteDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AthleteDocumentResponse response = athleteDocumentService.uploadDocument(
                email, mockFile, DocumentType.MEDICAL_CLEARANCE, "Mi Apto Medico Personalizado");

        assertNotNull(response);
        assertEquals("Mi Apto Medico Personalizado", response.description());
        assertEquals(DocumentType.MEDICAL_CLEARANCE, response.documentType());

        verify(athleteDocumentRepository, times(1)).save(any(AthleteDocument.class));
    }

    @Test
    void uploadDocument_WithoutDescription_MedicalClearance_RenamesFileCorrectly() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(athleteRepository.findByUserId(usuario.getId())).thenReturn(Optional.of(athlete));
        when(fileStorageService.uploadFile(any(), anyString())).thenReturn("athletes/10/medical_clearance/key.pdf");
        when(athleteDocumentRepository.save(any(AthleteDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AthleteDocumentResponse response = athleteDocumentService.uploadDocument(
                email, mockFile, DocumentType.MEDICAL_CLEARANCE, null);

        assertNotNull(response);
        assertNull(response.description());
        assertEquals(DocumentType.MEDICAL_CLEARANCE, response.documentType());

        verify(athleteDocumentRepository, times(1)).save(any(AthleteDocument.class));
    }

    @Test
    void uploadDocument_WithoutDescription_PaymentReceipt_RenamesFileCorrectly() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(athleteRepository.findByUserId(usuario.getId())).thenReturn(Optional.of(athlete));
        when(fileStorageService.uploadFile(any(), anyString())).thenReturn("athletes/10/payment_receipt/key.png");
        when(athleteDocumentRepository.save(any(AthleteDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile mockPaymentFile = new MockMultipartFile(
                "file",
                "payment_original.png",
                "image/png",
                "payment data".getBytes()
        );

        AthleteDocumentResponse response = athleteDocumentService.uploadDocument(
                email, mockPaymentFile, DocumentType.PAYMENT_RECEIPT, "   ");

        assertNotNull(response);
        assertEquals("   ", response.description());
        assertEquals(DocumentType.PAYMENT_RECEIPT, response.documentType());

        verify(athleteDocumentRepository, times(1)).save(any(AthleteDocument.class));
    }

    @Test
    void uploadDocument_UserNotFound() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> athleteDocumentService.uploadDocument(
                email, mockFile, DocumentType.MEDICAL_CLEARANCE, "Test Desc"));

        verify(usuarioRepository, times(1)).findByEmail(email);
        verifyNoInteractions(athleteRepository, fileStorageService, athleteDocumentRepository);
    }

    @Test
    void getMyDocuments_Success() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(athleteRepository.findByUserId(usuario.getId())).thenReturn(Optional.of(athlete));
        when(athleteDocumentRepository.findByAthleteId(athlete.getId())).thenReturn(List.of(document));

        List<AthleteDocumentResponse> docs = athleteDocumentService.getMyDocuments(email);

        assertNotNull(docs);
        assertEquals(1, docs.size());
        assertEquals(document.getId(), docs.get(0).id());

        verify(usuarioRepository, times(1)).findByEmail(email);
        verify(athleteRepository, times(1)).findByUserId(usuario.getId());
        verify(athleteDocumentRepository, times(1)).findByAthleteId(athlete.getId());
    }

    @Test
    void getAthleteDocuments_Success() {
        when(athleteDocumentRepository.findByAthleteId(10L)).thenReturn(List.of(document));

        List<AthleteDocumentResponse> docs = athleteDocumentService.getAthleteDocuments(10L);

        assertNotNull(docs);
        assertEquals(1, docs.size());

        verify(athleteDocumentRepository, times(1)).findByAthleteId(10L);
    }

    @Test
    void getDocumentById_Success() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(athleteRepository.findByUserId(usuario.getId())).thenReturn(Optional.of(athlete));
        when(athleteDocumentRepository.findById(100L)).thenReturn(Optional.of(document));

        AthleteDocumentResponse res = athleteDocumentService.getDocumentById(email, 100L);

        assertNotNull(res);
        assertEquals(document.getId(), res.id());
    }

    @Test
    void getDocumentById_DocumentNotFound() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(athleteRepository.findByUserId(usuario.getId())).thenReturn(Optional.of(athlete));
        when(athleteDocumentRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> athleteDocumentService.getDocumentById(email, 100L));
    }

    @Test
    void getDocumentById_AccessDenied() {
        Athlete otherAthlete = new Athlete();
        otherAthlete.setId(99L);
        document.setAthlete(otherAthlete); // mismatch

        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(athleteRepository.findByUserId(usuario.getId())).thenReturn(Optional.of(athlete));
        when(athleteDocumentRepository.findById(100L)).thenReturn(Optional.of(document));

        assertThrows(AccessDeniedException.class, () -> athleteDocumentService.getDocumentById(email, 100L));
    }

    @Test
    void getDocumentFile_Success() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(athleteRepository.findByUserId(usuario.getId())).thenReturn(Optional.of(athlete));
        when(athleteDocumentRepository.findById(100L)).thenReturn(Optional.of(document));
        when(fileStorageService.downloadFile(document.getFileKey())).thenReturn(new ByteArrayInputStream("data".getBytes()));

        InputStream res = athleteDocumentService.getDocumentFile(email, 100L);

        assertNotNull(res);
        verify(fileStorageService, times(1)).downloadFile(document.getFileKey());
    }

    @Test
    void deleteDocument_Success() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(usuario));
        when(athleteRepository.findByUserId(usuario.getId())).thenReturn(Optional.of(athlete));
        when(athleteDocumentRepository.findById(100L)).thenReturn(Optional.of(document));

        athleteDocumentService.deleteDocument(email, 100L);

        verify(fileStorageService, times(1)).deleteFile(document.getFileKey());
        verify(athleteDocumentRepository, times(1)).delete(document);
    }
}
