/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions

class CreateFunctionIntentionTest : RsIntentionTestBase(CreateFunctionIntention()) {
    fun `test unavailable on resolved function`() = doUnavailableTest("""
        fn foo() {}

        fn main() {
            /*caret*/foo();
        }
    """)

    fun `test unavailable on arguments`() = doUnavailableTest("""
        fn main() {
            foo(1/*caret*/);
        }
    """)

    fun `test unavailable on path argument`() = doUnavailableTest("""
        fn foo(a: u32) {}

        fn main() {
            foo(bar::baz/*caret*/);
        }
    """)

    fun `test create function`() = doAvailableTest("""
        fn main() {
            /*caret*/foo();
        }
    """, """
        fn main() {
            foo();
        }

        fn foo() {
            unimplemented!()/*caret*/
        }
    """)

    fun `test create function in an existing module`() = doAvailableTest("""
        mod foo {}

        fn main() {
            foo::bar/*caret*/();
        }
    """, """
        mod foo {
            pub(crate) fn bar() {
                unimplemented!()/*caret*/
            }
        }

        fn main() {
            foo::bar();
        }
    """)

    fun `test create function in an existing file`() = doAvailableTestWithFileTreeComplete("""
        //- main.rs
            mod foo;

            fn main() {
                foo::bar/*caret*/();
            }
        //- foo.rs
            fn test() {}
    """, """
        //- main.rs
            mod foo;

            fn main() {
                foo::bar();
            }
        //- foo.rs
            fn test() {}

            pub(crate) fn bar() {
                unimplemented!()
            }
    """)

    fun `test unresolved function call in a missing module`() = doUnavailableTest("""
        fn main() {
            foo::bar/*caret*/();
        }
    """)

    fun `test unresolved function call in a nested function`() = doAvailableTest("""
        fn main() {
            fn foo() {
                /*caret*/bar();
            }
        }
    """, """
        fn main() {
            fn foo() {
                bar();
            }
            fn bar() {
                unimplemented!()/*caret*/
            }
        }
    """)

    fun `test unresolved function call inside a module`() = doAvailableTest("""
        mod foo {
            fn main() {
                /*caret*/bar();
            }
        }
    """, """
        mod foo {
            fn main() {
                bar();
            }

            fn bar() {
                unimplemented!()/*caret*/
            }
        }
    """)

    fun `test simple parameters`() = doAvailableTest("""
        fn main() {
            let a = 5;
            foo/*caret*/(1, "hello", &a);
        }
    """, """
        fn main() {
            let a = 5;
            foo(1, "hello", &a);
        }

        fn foo(p0: i32, p1: &str, p2: &i32) {
            unimplemented!()
        }
    """)

    fun `test generic parameters`() = doAvailableTest("""
        trait Trait1 {}
        trait Trait2 {}

        fn foo<T, X, R: Trait1>(t1: T, t2: T, r: R) where T: Trait2 {
            bar/*caret*/(r, t1, t2);
        }
    """, """
        trait Trait1 {}
        trait Trait2 {}

        fn foo<T, X, R: Trait1>(t1: T, t2: T, r: R) where T: Trait2 {
            bar(r, t1, t2);
        }

        fn bar<T, R: Trait1>(p0: R, p1: T, p2: T) where T: Trait2 {
            unimplemented!()
        }
    """)

    fun `test complex generic constraints inside impl`() = doAvailableTest("""
        struct S<T>(T);
        trait Trait {}
        trait Trait2 {}

        impl<'a, 'b, T: 'a> S<T> where for<'c> T: Trait + Fn(&'c i32) {
            fn foo<R>(t: T, r: &R) where T: Trait2 + Trait, R: Trait + for<'d> Fn(&'d i32) {
                bar/*caret*/(t, r);
            }
        }
    """, """
        struct S<T>(T);
        trait Trait {}
        trait Trait2 {}

        impl<'a, 'b, T: 'a> S<T> where for<'c> T: Trait + Fn(&'c i32) {
            fn foo<R>(t: T, r: &R) where T: Trait2 + Trait, R: Trait + for<'d> Fn(&'d i32) {
                bar(t, r);
            }
        }

        fn bar<'a, R, T: 'a>(p0: T, p1: &R) where R: Trait + for<'d> Fn(&'d i32), T: Trait + Trait2, for<'c> T: Fn(&'c i32) + Trait {
            unimplemented!()
        }
    """)

    fun `test nested function generic parameters`() = doAvailableTest("""
        fn foo<T>() where T: Foo {
            fn bar<T>(t: T) where T: Bar {
                baz/*caret*/(t);
            }
        }
    """, """
        fn foo<T>() where T: Foo {
            fn bar<T>(t: T) where T: Bar {
                baz(t);
            }
            fn baz<T>(p0: T) where T: Bar {
                unimplemented!()
            }
        }
    """)
}
