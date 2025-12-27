package com.ratingsandreviews.application;

import com.ratingsandreviews.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApplicationServiceImplTest {
    @Mock
    private ApplicationRepositoryWrapper wrapper;

    @Mock
    private CacheService redisCacheService;

    @InjectMocks
    private ApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getApplication_returnsApplication() {
        String appId = UUID.randomUUID().toString();
        Application app = new Application();
        when(redisCacheService.get(anyString(), eq(Application.class))).thenReturn(null); // Cache miss
        when(wrapper.getApplication(appId)).thenReturn(app);
        Application result = service.getApplication(appId);
        assertNotNull(result);
        verify(wrapper).getApplication(appId);
        verify(redisCacheService).put(anyString(), eq(app)); // Verify cache is updated
    }

    @Test
    void getApplications_returnsPage() {
        Page<Application> page = new PageImpl<>(List.of(new Application()));
        when(redisCacheService.get(anyString(), eq(Page.class))).thenReturn(null); // Cache miss
        when(wrapper.getApplications(anyString(), anyString(), any(Pageable.class))).thenReturn(page);
        Page<Application> result = service.getApplications("all", "", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        verify(redisCacheService).put(anyString(), eq(page)); // Verify cache is updated
    }

    @Test
    void installApplication_logsInstall() {
        // Just ensure no exception is thrown
        service.installApplication("appId");
    }

    @Test
    void installApplications_parallelInstall() {
        List<String> ids = List.of("id1", "id2");
        service.installApplications(ids);
    }

    @Test
    void uninstallApplication_logsUninstall() {
        service.uninstallApplication("appId");
    }

    @Test
    void uninstallApplications_parallelUninstall() {
        List<String> ids = List.of("id1", "id2");
        service.uninstallApplications(ids);
    }
}
