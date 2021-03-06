/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.integtests

import org.apache.tools.ant.taskdefs.Expand
import org.gradle.integtests.fixtures.GradleDistribution
import org.gradle.integtests.fixtures.GradleDistributionExecuter
import org.gradle.util.AntUtil
import org.gradle.util.GradleVersion
import org.gradle.util.TestFile
import org.junit.Rule
import org.junit.Test
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat
import org.gradle.util.PreconditionVerifier
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition

class DistributionIntegrationTest {
    @Rule public final GradleDistribution dist = new GradleDistribution()
    @Rule public final GradleDistributionExecuter executer = new GradleDistributionExecuter()
    @Rule public final PreconditionVerifier preconditionVerifier = new PreconditionVerifier()
    private String version = GradleVersion.current().version

    @Test
    void binZipContents() {
        TestFile binZip = dist.distributionsDir.file("gradle-$version-bin.zip")
        binZip.usingNativeTools().unzipTo(dist.testDir)
        TestFile contentsDir = dist.testDir.file("gradle-$version")

        checkMinimalContents(contentsDir)

        // Extra stuff
        contentsDir.file('src').assertDoesNotExist()
        contentsDir.file('samples').assertDoesNotExist()
        contentsDir.file('docs').assertDoesNotExist()
    }

    @Test
    void allZipContents() {
        TestFile binZip = dist.distributionsDir.file("gradle-$version-all.zip")
        binZip.usingNativeTools().unzipTo(dist.testDir)
        TestFile contentsDir = dist.testDir.file("gradle-$version")

        checkMinimalContents(contentsDir)

        // Source
        contentsDir.file('src/org/gradle/api/Project.java').assertIsFile()
        contentsDir.file('src/org/gradle/initialization/defaultBuildSourceScript.txt').assertIsFile()
        contentsDir.file('src/org/gradle/gradleplugin/userinterface/swing/standalone/BlockingApplication.java').assertIsFile()
        contentsDir.file('src/org/gradle/wrapper/WrapperExecutor.java').assertIsFile()

        // Samples
        contentsDir.file('samples/java/quickstart/build.gradle').assertIsFile()

        // Javadoc
        contentsDir.file('docs/javadoc/index.html').assertIsFile()
        contentsDir.file('docs/javadoc/index.html').assertContents(containsString("Gradle API ${version}"))
        contentsDir.file('docs/javadoc/org/gradle/api/Project.html').assertIsFile()

        // Groovydoc
        contentsDir.file('docs/groovydoc/index.html').assertIsFile()
        contentsDir.file('docs/groovydoc/org/gradle/api/Project.html').assertIsFile()
        contentsDir.file('docs/groovydoc/org/gradle/api/tasks/bundling/Zip.html').assertIsFile()

        // Userguide
        contentsDir.file('docs/userguide/userguide.html').assertIsFile()
        contentsDir.file('docs/userguide/userguide.html').assertContents(containsString("<h3 class=\"releaseinfo\">Version ${version}</h3>"))
        contentsDir.file('docs/userguide/userguide_single.html').assertIsFile()
        contentsDir.file('docs/userguide/userguide_single.html').assertContents(containsString("<h3 class=\"releaseinfo\">Version ${version}</h3>"))
//        contentsDir.file('docs/userguide/userguide.pdf').assertIsFile()

        // DSL reference
        contentsDir.file('docs/dsl/index.html').assertIsFile()
        contentsDir.file('docs/dsl/index.html').assertContents(containsString("<title>Gradle DSL Version ${version}</title>"))
    }

    private void checkMinimalContents(TestFile contentsDir) {
        // Check it can be executed
        executer.inDirectory(contentsDir).usingExecutable('bin/gradle').withTaskList().run()

        // Scripts
        contentsDir.file('bin/gradle').assertIsFile()
        contentsDir.file('bin/gradle.bat').assertIsFile()

        // Top level files
        contentsDir.file('LICENSE').assertIsFile()

        // Core libs
        def coreLibs = contentsDir.file("lib").listFiles().findAll { it.name.startsWith("gradle-") }
        assert coreLibs.size() == 10
        coreLibs.each { assertIsGradleJar(it) }
        def wrapperJar = contentsDir.file("lib/gradle-wrapper-${version}.jar")
        assert wrapperJar.length() < 20 * 1024; // wrapper needs to be small. Let's check it's smaller than some arbitrary 'small' limit

        // Plugins
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-core-impl-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-plugins-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-ide-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-scala-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-code-quality-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-antlr-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-announce-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-jetty-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-sonar-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-maven-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-osgi-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-signing-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-cpp-${version}.jar"))
        assertIsGradleJar(contentsDir.file("lib/plugins/gradle-ear-${version}.jar"))

        // Docs
        contentsDir.file('getting-started.html').assertIsFile()
        
        // Jars that must not be shipped
        assert !contentsDir.file("lib/tools.jar").exists()
        assert !contentsDir.file("lib/plugins/tools.jar").exists()
    }

    private void assertIsGradleJar(TestFile jar) {
        jar.assertIsFile()
        assertThat(jar.manifest.mainAttributes.getValue('Implementation-Version'), equalTo(version))
        assertThat(jar.manifest.mainAttributes.getValue('Implementation-Title'), equalTo('Gradle'))
    }

    @Test @Requires(TestPrecondition.NOT_WINDOWS)
    void sourceZipContents() {
        TestFile srcZip = dist.distributionsDir.file("gradle-$version-src.zip")
        srcZip.usingNativeTools().unzipTo(dist.testDir)
        TestFile contentsDir = dist.testDir.file("gradle-$version")

        // Build self using wrapper in source distribution
        executer.withDeprecationChecksDisabled().inDirectory(contentsDir).usingExecutable('gradlew').withTasks('binZip').run()

        File binZip = contentsDir.file('build/distributions').listFiles()[0]
        Expand unpack = new Expand()
        unpack.src = binZip
        unpack.dest = contentsDir.file('build/distributions/unzip')
        AntUtil.execute(unpack)
        TestFile unpackedRoot = new TestFile(contentsDir.file('build/distributions/unzip').listFiles()[0])

        // Make sure the build distribution does something useful
        unpackedRoot.file("bin/gradle").assertIsFile()
        // todo run something with the gradle build by the source dist
    }
}
