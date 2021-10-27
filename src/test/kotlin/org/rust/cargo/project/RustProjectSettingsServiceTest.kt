/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project

import com.intellij.testFramework.LightPlatformTestCase
import org.intellij.lang.annotations.Language
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import org.rust.cargo.project.settings.RustProjectSettingsService.MacroExpansionEngine
import org.rust.cargo.project.settings.impl.RustProjectSettingsServiceImpl
import org.rust.cargo.project.settings.impl.XML_FORMAT_VERSION
import org.rust.cargo.toolchain.ExternalLinter
import org.rust.cargo.toolchain.RsLocalToolchain
import org.rust.ide.annotator.RsLongExternalLinterRunNotifier.Companion.DEFAULT_MAX_DURATION
import org.rust.openapiext.elementFromXmlString
import org.rust.openapiext.toXmlString
import java.nio.file.Paths

@RunWith(JUnit38ClassRunner::class) // TODO: drop the annotation when issue with Gradle test scanning go away
class RustProjectSettingsServiceTest : LightPlatformTestCase() {

    fun `test serialization`() {
        val service = RustProjectSettingsServiceImpl(project)

        @Language("XML")
        val text = """
            <RustProjectSettings>
              <option name="autoUpdateEnabled" value="false" />
              <option name="compileAllTargets" value="false" />
              <option name="doctestInjectionEnabled" value="false" />
              <option name="explicitPathToStdlib" value="/stdlib" />
              <option name="externalLinter" value="Clippy" />
              <option name="externalLinterArguments" value="--no-default-features" />
              <option name="macroExpansionEngine" value="DISABLED" />
              <option name="runExternalLinterOnTheFly" value="false" />
              <option name="externalLinterOnTheFlyMaxDuration" value="9999" />
              <option name="runRustfmtOnSave" value="true" />
              <option name="toolchainHomeDirectory" value="/" />
              <option name="useOffline" value="true" />
              <option name="useRustfmt" value="true" />
              <option name="version" value="2" />
            </RustProjectSettings>
        """.trimIndent()
        service.loadState(elementFromXmlString(text))

        val actual = service.state.toXmlString()
        assertEquals(text, actual)

        assertEquals(XML_FORMAT_VERSION, service.version)
        assertEquals(RsLocalToolchain(Paths.get("/")), service.toolchain)
        assertEquals(false, service.autoUpdateEnabled)
        assertEquals(ExternalLinter.CLIPPY, service.externalLinter)
        assertEquals("/stdlib", service.explicitPathToStdlib)
        assertEquals(true, service.runExternalLinterOnTheFly)
        assertEquals(9999, service.externalLinterOnTheFlyMaxDuration)
        assertEquals("--no-default-features", service.externalLinterArguments)
        assertEquals(false, service.compileAllTargets)
        assertEquals(true, service.useOffline)
        assertEquals(MacroExpansionEngine.DISABLED, service.macroExpansionEngine)
        assertEquals(false, service.doctestInjectionEnabled)
        assertEquals(true, service.useRustfmt)
        assertEquals(true, service.runRustfmtOnSave)
    }

    fun `test update from version 1`() {
        val service = RustProjectSettingsServiceImpl(project)

        @Language("XML")
        val text = """
            <RustProjectSettings>
              <option name="autoUpdateEnabled" value="true" />
              <option name="compileAllTargets" value="false" />
              <option name="useCargoCheckAnnotator" value="true" />
              <option name="expandMacros" value="false" />
            </RustProjectSettings>
        """.trimIndent()
        service.loadState(elementFromXmlString(text))

        @Language("XML")
        val expected = """
            <RustProjectSettings>
              <option name="compileAllTargets" value="false" />
              <option name="macroExpansionEngine" value="DISABLED" />
              <option name="runExternalLinterOnTheFly" value="false" />
              <option name="externalLinterOnTheFlyMaxDuration" value="9999" />
              <option name="version" value="2" />
            </RustProjectSettings>
        """.trimIndent()
        val actual = service.state.toXmlString()
        assertEquals(expected, actual)

        assertEquals(false, service.runExternalLinterOnTheFly)
        assertEquals(9999, service.externalLinterOnTheFlyMaxDuration)
        assertEquals("", service.externalLinterArguments)
        assertEquals(MacroExpansionEngine.DISABLED, service.macroExpansionEngine)
    }
}
