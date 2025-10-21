package com.ocbc.finance.service;

import com.ocbc.finance.dto.contract.ContractUploadResponse;
import com.ocbc.finance.entity.OriginalContract;
import com.ocbc.finance.repository.ContractRepository;
import com.ocbc.finance.repository.OriginalContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ContractService单元测试
 * 
 * @author OCBC Finance Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private OriginalContractRepository originalContractRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private GeminiAiService geminiAiService;

    @Mock
    private Gemma3AiService gemma3AiService;

    @InjectMocks
    private ContractService contractService;

    private MockMultipartFile testPdfFile;

    @BeforeEach
    void setUp() {
        // 创建测试PDF文件
        testPdfFile = new MockMultipartFile(
            "file", 
            "test-contract.pdf", 
            "application/pdf", 
            createTestPdfBytes()
        );
    }

    @Test
    void testUploadAndParseContract_WithGeminiAI_Success() {
        // Given
        ReflectionTestUtils.setField(contractService, "aiProvider", "gemini");
        
        OriginalContract savedContract = new OriginalContract();
        savedContract.setId(1L);
        savedContract.setFileName("test-contract.pdf");
        
        Map<String, Object> aiResult = new HashMap<>();
        aiResult.put("contractNo", "TEST001");
        aiResult.put("contractName", "测试合同");
        aiResult.put("partyA", "OCBC银行");
        
        when(originalContractRepository.save(any(OriginalContract.class))).thenReturn(savedContract);
        when(geminiAiService.isAvailable()).thenReturn(true);
        when(geminiAiService.parseContractWithAI(anyString())).thenReturn(aiResult);

        // When
        ContractUploadResponse response = contractService.uploadAndParseContract(testPdfFile, "testuser");

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getFileId());
        assertEquals("test-contract.pdf", response.getFileName());
        assertTrue(response.getAiParsed());
        assertEquals("使用Gemini AI解析成功", response.getParseMessage());
        assertNotNull(response.getExtractedInfo());
        assertEquals("TEST001", response.getExtractedInfo().get("contractNo"));
        
        verify(originalContractRepository).save(any(OriginalContract.class));
        verify(geminiAiService).parseContractWithAI(anyString());
    }

    @Test
    void testUploadAndParseContract_WithGemma3AI_Success() {
        // Given
        ReflectionTestUtils.setField(contractService, "aiProvider", "gemma3");
        
        OriginalContract savedContract = new OriginalContract();
        savedContract.setId(1L);
        savedContract.setFileName("test-contract.pdf");
        
        Map<String, Object> aiResult = new HashMap<>();
        aiResult.put("contractNo", "TEST002");
        aiResult.put("contractName", "测试合同2");
        aiResult.put("partyA", "OCBC银行");
        
        when(originalContractRepository.save(any(OriginalContract.class))).thenReturn(savedContract);
        when(gemma3AiService.isAvailable()).thenReturn(true);
        when(gemma3AiService.parseContractWithAI(anyString())).thenReturn(aiResult);

        // When
        ContractUploadResponse response = contractService.uploadAndParseContract(testPdfFile, "testuser");

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getFileId());
        assertTrue(response.getAiParsed());
        assertEquals("使用Gemma3 AI解析成功", response.getParseMessage());
        assertEquals("TEST002", response.getExtractedInfo().get("contractNo"));
        
        verify(originalContractRepository).save(any(OriginalContract.class));
        verify(gemma3AiService).parseContractWithAI(anyString());
    }

    @Test
    void testUploadAndParseContract_AIFallbackToKeywords() {
        // Given
        ReflectionTestUtils.setField(contractService, "aiProvider", "gemini");
        
        OriginalContract savedContract = new OriginalContract();
        savedContract.setId(1L);
        savedContract.setFileName("test-contract.pdf");
        
        when(originalContractRepository.save(any(OriginalContract.class))).thenReturn(savedContract);
        when(geminiAiService.isAvailable()).thenReturn(true);
        when(geminiAiService.parseContractWithAI(anyString()))
            .thenThrow(new RuntimeException("AI服务超时"));

        // When
        ContractUploadResponse response = contractService.uploadAndParseContract(testPdfFile, "testuser");

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getFileId());
        assertFalse(response.getAiParsed());
        assertEquals("使用关键字匹配解析成功", response.getParseMessage());
        
        verify(originalContractRepository).save(any(OriginalContract.class));
        verify(geminiAiService).parseContractWithAI(anyString());
    }

    @Test
    void testUploadAndParseContract_AIServiceNotAvailable() {
        // Given
        ReflectionTestUtils.setField(contractService, "aiProvider", "gemini");
        
        OriginalContract savedContract = new OriginalContract();
        savedContract.setId(1L);
        savedContract.setFileName("test-contract.pdf");
        
        when(originalContractRepository.save(any(OriginalContract.class))).thenReturn(savedContract);
        when(geminiAiService.isAvailable()).thenReturn(false);

        // When
        ContractUploadResponse response = contractService.uploadAndParseContract(testPdfFile, "testuser");

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getFileId());
        assertFalse(response.getAiParsed());
        assertEquals("使用关键字匹配解析成功", response.getParseMessage());
        
        verify(originalContractRepository).save(any(OriginalContract.class));
        verify(geminiAiService, never()).parseContractWithAI(anyString());
    }

    @Test
    void testUploadAndParseContract_FallbackFromGeminiToGemma3() {
        // Given
        ReflectionTestUtils.setField(contractService, "aiProvider", "gemini");
        
        OriginalContract savedContract = new OriginalContract();
        savedContract.setId(1L);
        savedContract.setFileName("test-contract.pdf");
        
        Map<String, Object> gemma3Result = new HashMap<>();
        gemma3Result.put("contractNo", "FALLBACK001");
        gemma3Result.put("contractName", "降级测试合同");
        
        when(originalContractRepository.save(any(OriginalContract.class))).thenReturn(savedContract);
        when(geminiAiService.isAvailable()).thenReturn(true);
        when(geminiAiService.parseContractWithAI(anyString()))
            .thenThrow(new RuntimeException("Gemini服务异常"));
        when(gemma3AiService.isAvailable()).thenReturn(true);
        when(gemma3AiService.parseContractWithAI(anyString())).thenReturn(gemma3Result);

        // When
        ContractUploadResponse response = contractService.uploadAndParseContract(testPdfFile, "testuser");

        // Then
        assertNotNull(response);
        assertTrue(response.getAiParsed());
        assertEquals("主要AI服务失败，使用Gemma3 AI解析成功", response.getParseMessage());
        assertEquals("FALLBACK001", response.getExtractedInfo().get("contractNo"));
        
        verify(geminiAiService).parseContractWithAI(anyString());
        verify(gemma3AiService).parseContractWithAI(anyString());
    }

    @Test
    void testUploadAndParseContract_InvalidFileFormat() {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file", 
            "test.txt", 
            "text/plain", 
            "test content".getBytes()
        );

        // When & Then
        assertThrows(Exception.class, () -> {
            contractService.uploadAndParseContract(invalidFile, "testuser");
        });
    }

    /**
     * 创建测试PDF字节数据
     * 这里创建一个最小的PDF文件结构
     */
    private byte[] createTestPdfBytes() {
        // 创建一个最小的PDF文件结构用于测试
        String pdfContent = "%PDF-1.4\n" +
            "1 0 obj\n" +
            "<<\n" +
            "/Type /Catalog\n" +
            "/Pages 2 0 R\n" +
            ">>\n" +
            "endobj\n" +
            "2 0 obj\n" +
            "<<\n" +
            "/Type /Pages\n" +
            "/Kids [3 0 R]\n" +
            "/Count 1\n" +
            ">>\n" +
            "endobj\n" +
            "3 0 obj\n" +
            "<<\n" +
            "/Type /Page\n" +
            "/Parent 2 0 R\n" +
            "/MediaBox [0 0 612 792]\n" +
            ">>\n" +
            "endobj\n" +
            "xref\n" +
            "0 4\n" +
            "0000000000 65535 f \n" +
            "0000000010 00000 n \n" +
            "0000000079 00000 n \n" +
            "0000000173 00000 n \n" +
            "trailer\n" +
            "<<\n" +
            "/Size 4\n" +
            "/Root 1 0 R\n" +
            ">>\n" +
            "startxref\n" +
            "253\n" +
            "%%EOF";
        
        return pdfContent.getBytes();
    }
}
