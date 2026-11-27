package com.hrms.backend.security;

import com.hrms.backend.entity.PayrollBatch.BatchStatus;
import com.hrms.backend.repository.PayrollBatchRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class PayrollLockFilter extends OncePerRequestFilter {

    private static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("\\b(20\\d{2}-(?:0[1-9]|1[0-2]))\\b");

    private final PayrollBatchRepository payrollBatchRepository;

    public PayrollLockFilter(PayrollBatchRepository payrollBatchRepository) {
        this.payrollBatchRepository = payrollBatchRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        boolean isWriteMethod = "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method);
        boolean isProtectedModule = uri.startsWith("/api/v1/attendance")
                || uri.startsWith("/api/v1/salary")
                || (uri.startsWith("/api/v1/payroll")
                    && !uri.startsWith("/api/v1/payroll/calculate-single")
                    && !uri.startsWith("/api/v1/payroll/batches"));

        if (!isWriteMethod || !isProtectedModule) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip body wrapping for multipart uploads
        String contentType = request.getContentType();
        HttpServletRequest requestToPass = request;
        String bodyText = "";

        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
            CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
            requestToPass = wrappedRequest;
            bodyText = wrappedRequest.getCachedBodyAsString();
        }

        String targetMonth = extractYearMonth(uri, request.getQueryString(), bodyText);

        // Live attendance punches without a date belong to the current month
        if (targetMonth == null && uri.startsWith("/api/v1/attendance")) {
            targetMonth = YearMonth.now().toString();
        }

        if (targetMonth != null
                && payrollBatchRepository.existsByPayrollMonthAndStatus(targetMonth, BatchStatus.LOCKED)) {

            String origin = request.getHeader("Origin");
            if (origin != null && response.getHeader("Access-Control-Allow-Origin") == null) {
                response.setHeader("Access-Control-Allow-Origin", origin);
            }

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Payroll batch for "
                            + targetMonth
                            + " is LOCKED. Modifications to salary or attendance for this month are prohibited.\"}"
            );
            return;
        }

        filterChain.doFilter(requestToPass, response);
    }

    private String extractYearMonth(String uri, String query, String body) {
        for (String source : new String[]{uri, query, body}) {
            if (source == null || source.isBlank()) continue;
            Matcher matcher = YEAR_MONTH_PATTERN.matcher(source);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    // Caches the JSON request body so @RequestBody still reads it normally in controllers
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        String getCachedBodyAsString() {
            return new String(cachedBody, StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Not used for synchronous servlet requests
                }

                @Override
                public int read() {
                    return bais.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}