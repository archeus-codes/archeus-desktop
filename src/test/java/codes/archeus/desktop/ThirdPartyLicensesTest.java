/*
 * Copyright 2026 Deiondre Andrews (Demonic722)
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package codes.archeus.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Keeps THIRD-PARTY-LICENSES in step with the libraries the app ships with, which Maven lists in
 * {@code target/runtime-dependencies.txt} before the tests run.
 */
class ThirdPartyLicensesTest {
	private static final Path DEPENDENCIES = Path.of("target", "runtime-dependencies.txt");
	private static final Path LICENSES = Path.of("THIRD-PARTY-LICENSES");
	/** A listed dependency, such as {@code org.slf4j:slf4j-api:jar:2.0.20:compile}: its group, then its artifact. */
	private static final Pattern DEPENDENCY = Pattern.compile("^\\s+[\\w.-]+:([\\w.-]+):");

	@Test
	void thirdPartyLicenses_everyRuntimeDependency_isNamed() throws IOException {
		if (System.getenv("CI") == null) {
			assumeThat(DEPENDENCIES).as("listed by Maven before the tests, but not in a run from an IDE").exists();
		} else {
			assertThat(DEPENDENCIES).as("listed by Maven before the tests, which CI always runs").exists();
		}
		String licenses = Files.readString(LICENSES);

		List<String> artifacts = Files.readAllLines(DEPENDENCIES).stream()
				.map(DEPENDENCY::matcher)
				.filter(Matcher::find)
				.map(match -> match.group(1))
				.distinct()
				.toList();

		assertThat(artifacts).as("the runtime dependencies Maven listed").isNotEmpty();
		assertThat(artifacts)
				.as("libraries the app ships with that THIRD-PARTY-LICENSES doesn't name")
				.filteredOn(artifact -> !names(licenses, artifact))
				.isEmpty();
	}

	/**
	 * Whether the text names the artifact as a whole, so {@code ikonli-core} doesn't count for {@code ikonli-core2}.
	 */
	private static boolean names(String text, String artifact) {
		return Pattern.compile("(?<![\\w.-])" + Pattern.quote(artifact) + "(?![\\w.-])").matcher(text).find();
	}
}
