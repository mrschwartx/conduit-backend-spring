package com.conduit.backend.shared.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.conduit.backend.shared.config.AppProperties;
import com.conduit.backend.shared.errors.ErrorJson;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class BodySizeLimitFilter extends OncePerRequestFilter {

	private final long limit;

	public BodySizeLimitFilter(AppProperties properties) {
		this.limit = properties.maxBodyBytes();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (request.getContentLengthLong() > limit) {
			ErrorJson.write(response, 413, Map.of("body", List.of("is too large")));
			return;
		}
		chain.doFilter(new LimitedRequest(request, limit), response);
	}

	private static final class LimitedRequest extends HttpServletRequestWrapper {

		private final long limit;
		private ServletInputStream stream;

		LimitedRequest(HttpServletRequest request, long limit) {
			super(request);
			this.limit = limit;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			if (stream == null) {
				stream = new LimitedStream(super.getInputStream(), limit);
			}
			return stream;
		}

		@Override
		public BufferedReader getReader() throws IOException {
			String encoding = getCharacterEncoding();
			Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
			return new BufferedReader(new InputStreamReader(getInputStream(), charset));
		}
	}

	private static final class LimitedStream extends ServletInputStream {

		private final ServletInputStream delegate;
		private final long limit;
		private long count;

		LimitedStream(ServletInputStream delegate, long limit) {
			this.delegate = delegate;
			this.limit = limit;
		}

		private void count(long n) throws IOException {
			count += n;
			if (count > limit) {
				throw new BodyTooLargeException(limit);
			}
		}

		@Override
		public int read() throws IOException {
			int b = delegate.read();
			if (b >= 0) {
				count(1);
			}
			return b;
		}

		@Override
		public int read(byte[] buffer, int off, int len) throws IOException {
			int n = delegate.read(buffer, off, len);
			if (n > 0) {
				count(n);
			}
			return n;
		}

		@Override
		public boolean isFinished() {
			return delegate.isFinished();
		}

		@Override
		public boolean isReady() {
			return delegate.isReady();
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			delegate.setReadListener(readListener);
		}
	}
}
