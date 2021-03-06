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



package org.gradle.internal.concurrent

import static org.hamcrest.Matchers.*

import java.util.concurrent.ExecutorService
import org.gradle.util.JUnit4GroovyMockery
import org.gradle.util.MultithreadedTestCase
import org.jmock.integration.junit4.JMock
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import static org.junit.Assert.*
import java.util.concurrent.TimeUnit
import org.gradle.internal.concurrent.DefaultExecutorFactory

@RunWith(JMock)
class DefaultExecutorFactoryTest extends MultithreadedTestCase {
    private final JUnit4GroovyMockery context = new JUnit4GroovyMockery()
    private final DefaultExecutorFactory factory = new DefaultExecutorFactory() {
        def ExecutorService createExecutor(String displayName) {
            return getExecutor()
        }
    }

    @After
    public void tearDown() {
        factory.stop()
    }

    @Test
    public void stopBlocksUntilAllJobsAreComplete() {
        Runnable runnable = context.mock(Runnable.class)

        context.checking {
            one(runnable).run()
            will {
                syncAt(1)
            }
        }

        def executor = factory.create('<display-name>')
        executor.execute(runnable)

        run {
            expectBlocksUntil(1) {
                executor.stop()
            }
        }
    }
    
    @Test
    public void factoryStopBlocksUntilAllJobsAreComplete() {
        Runnable runnable = context.mock(Runnable.class)

        context.checking {
            one(runnable).run()
            will {
                syncAt(1)
            }
        }

        def executor = factory.create('<display-name>')
        executor.execute(runnable)

        run {
            expectBlocksUntil(1) {
                factory.stop()
            }
        }
    }

    @Test
    public void stopRethrowsFirstExecutionException() {
        Runnable runnable1 = context.mock(Runnable.class)
        Runnable runnable2 = context.mock(Runnable.class)
        RuntimeException failure1 = new RuntimeException()
        RuntimeException failure2 = new RuntimeException()

        context.checking {
            one(runnable1).run()
            will {
                throw failure1
            }
            one(runnable2).run()
            will {
                throw failure2
            }
        }

        def executor = factory.create('<display-name>')
        executor.execute(runnable1)
        executor.execute(runnable2)

        try {
            executor.stop()
            org.junit.Assert.fail()
        } catch (RuntimeException e) {
            org.junit.Assert.assertThat(e, org.hamcrest.Matchers.sameInstance(failure1))
        }
    }

    @Test
    public void stopThrowsExceptionOnTimeout() {
        Runnable runnable = context.mock(Runnable.class)

        context.checking {
            one(runnable).run()
            will {
                Thread.sleep(1000)
            }
        }

        def executor = factory.create('<display-name>')
        executor.execute(runnable)

        expectTimesOut(500, TimeUnit.MILLISECONDS) {
            try {
                executor.stop(500, TimeUnit.MILLISECONDS)
                org.junit.Assert.fail()
            } catch (IllegalStateException e) {
                org.junit.Assert.assertThat(e.message, org.hamcrest.Matchers.equalTo('Timeout waiting for concurrent jobs to complete.'))
            }
        }
    }

    @Test
    public void cannotStopExecutorFromAnExecutorThread() {
        Runnable runnable = context.mock(Runnable.class)

        def executor = factory.create('<display-name>')

        context.checking {
            one(runnable).run()
            will {
                executor.stop()
            }
        }

        executor.execute(runnable)

        try {
            executor.stop()
            org.junit.Assert.fail()
        } catch (IllegalStateException e) {
            org.junit.Assert.assertThat(e.message, org.hamcrest.Matchers.equalTo('Cannot stop this executor from an executor thread.'))
        }

    }
}
