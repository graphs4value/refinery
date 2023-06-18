/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server;

import java.util.Objects;

import org.eclipse.xtext.web.server.IServiceResult;

public class PongResult implements IServiceResult {
	private String pong;

	public PongResult(String pong) {
		super();
		this.pong = pong;
	}

	public String getPong() {
		return pong;
	}

	public void setPong(String pong) {
		this.pong = pong;
	}

	@Override
	public int hashCode() {
		return Objects.hash(pong);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PongResult other = (PongResult) obj;
		return Objects.equals(pong, other.pong);
	}

	@Override
	public String toString() {
		return "PongResult [pong=" + pong + "]";
	}
}
