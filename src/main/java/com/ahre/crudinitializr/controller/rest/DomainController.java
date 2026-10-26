package com.ahre.crudinitializr.controller.rest;

import com.ahre.crudinitializr.service.DomainService;
import com.ahre.crudinitializr.utils.FileUtils;
import com.ahre.crudinitializr.utils.ZipUtils;
import com.ahre.crudinitializr.vo.Domain;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/domain")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService domainService;
    private final FileUtils fileUtils;

    @PostMapping
    public void processDomain(@RequestBody Domain domain, HttpServletResponse response) throws IOException {
        Path file = domainService.processDomain(domain);

        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment;filename=project.zip");
            response.setStatus(HttpServletResponse.SC_OK);
            ZipUtils.zipFolder(file, response.getOutputStream());
        } finally {
            fileUtils.deleteQuietly(file);
        }
    }
}
