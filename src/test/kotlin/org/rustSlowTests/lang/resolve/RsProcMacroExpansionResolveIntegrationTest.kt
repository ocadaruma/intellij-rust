/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rustSlowTests.lang.resolve

import com.intellij.util.ThrowableRunnable
import org.rust.ExpandMacros
import org.rust.MinRustcVersion
import org.rust.cargo.RsWithToolchainTestBase
import org.rust.cargo.project.model.impl.testCargoProjects
import org.rust.cargo.toolchain.wsl.RsWslToolchain
import org.rust.fileTree
import org.rust.ide.experiments.RsExperiments
import org.rust.lang.core.macros.MacroExpansionScope
import org.rust.lang.core.psi.RsMethodCall
import org.rust.openapiext.RsPathManager
import org.rust.openapiext.pathAsPath
import org.rust.openapiext.runWithEnabledFeatures

@MinRustcVersion("1.46.0")
@ExpandMacros(MacroExpansionScope.WORKSPACE)
class RsProcMacroExpansionResolveIntegrationTest : RsWithToolchainTestBase() {
    fun `test 2 cargo projects (proc macro is a separate cargo project)`() = runWithProcMacrosEnabled {
        fileTree {
            dir("my_proc_macro") {
                toml("Cargo.toml", """
                    [package]
                    name = "my_proc_macro"
                    version = "1.0.0"
                    edition = "2018"

                    [lib]
                    proc-macro = true

                    [dependencies]
                """)
                dir("src") {
                    rust("lib.rs", """
                        extern crate proc_macro;
                        use proc_macro::TokenStream;

                        #[proc_macro]
                        pub fn my_macro(input: TokenStream) -> TokenStream {
                            return input;
                        }
                    """)
                }
            }
            dir("mylib") {
                toml("Cargo.toml", """
                    [package]
                    name = "mylib"
                    version = "1.0.0"
                    edition = "2018"

                    [dependencies]
                    my_proc_macro = { path = "../my_proc_macro" }
                """)
                dir("src") {
                    rust("lib.rs", """
                        use my_proc_macro::my_macro;

                        struct Foo;
                        impl Foo {
                            fn bar(&self) {}
                        }     //X

                        my_macro! {
                            fn foo() -> Foo { Foo }
                        }

                        fn main() {
                            foo().bar()
                        }       //^
                    """)
                }
            }
        }.run {
            val prj = create(project, cargoProjectDirectory)
            project.testCargoProjects.attachCargoProjects(
                cargoProjectDirectory.pathAsPath.resolve("my_proc_macro/Cargo.toml"),
                cargoProjectDirectory.pathAsPath.resolve("mylib/Cargo.toml")
            )
            prj.checkReferenceIsResolved<RsMethodCall>("mylib/src/lib.rs")
        }
    }

    fun `test from crates_io`() = runWithProcMacrosEnabled {
        buildProject {
            toml("Cargo.toml", """
                [package]
                name = "mylib"
                version = "1.0.0"
                edition = "2018"

                [dependencies]
                proc-macro-id = "=1.0.1"
            """)
            dir("src") {
                rust("lib.rs", """
                    use proc_macro_id::id;

                    struct Foo;
                    impl Foo {
                        fn bar(&self) {}
                    }     //X

                    id! {
                        fn foo() -> Foo { Foo }
                    }

                    fn main() {
                        foo().bar()
                    }       //^
                """)
            }
        }.checkReferenceIsResolved<RsMethodCall>("src/lib.rs")
    }

    override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
        if (RsPathManager.nativeHelper(rustupFixture.toolchain is RsWslToolchain) == null &&
            System.getenv("CI") == null) {
            System.err.println("SKIP \"$name\": no native-helper executable")
            return
        }
        super.runTestRunnable(testRunnable)
    }

    private fun <T> runWithProcMacrosEnabled(action: () -> T): T {
        return runWithEnabledFeatures(RsExperiments.EVALUATE_BUILD_SCRIPTS, RsExperiments.PROC_MACROS) { action() }
    }
}
