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

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/domain")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService domainService;
    private final FileUtils fileUtils;

    @PostMapping
    public void processDomain(@RequestBody Domain domain, HttpServletResponse response) throws IOException {
        Path file = domainService.processDomain(domain);

        response.setHeader("Content-Disposition", "attachment;filename=download.zip");
        response.setStatus(HttpServletResponse.SC_OK);

        ZipOutputStream zippedOut = new ZipOutputStream(response.getOutputStream());
        ZipUtils.processFolder(file.toFile(), zippedOut, file.toFile().getPath().length() + 1);
        zippedOut.finish();
    }
}
