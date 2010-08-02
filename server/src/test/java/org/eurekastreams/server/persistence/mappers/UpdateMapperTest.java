/*
 * Copyright (c) 2009-2010 Lockheed Martin Corporation
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
package org.eurekastreams.server.persistence.mappers;

import static org.junit.Assert.assertTrue;

import javax.persistence.EntityManager;

import org.eurekastreams.server.persistence.mappers.requests.PersistenceRequest;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

/**
 * Test class for UpdateMapper class.
 *
 */
public class UpdateMapperTest
{
    /**
     * mock context.
     */
    private final Mockery context = new JUnit4Mockery()
    {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    /**
     * Test execute method.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testExecute()
    {
        final EntityManager entityManager = context.mock(EntityManager.class);
        final PersistenceRequest req = context.mock(PersistenceRequest.class);

        UpdateMapper sut = new UpdateMapper();
        sut.setEntityManager(entityManager);

        context.checking(new Expectations()
        {
            {
                oneOf(entityManager).flush();
            }
        });

        assertTrue(sut.execute(req));
        context.assertIsSatisfied();
    }

    /**
     * Test execute method with a wrapped updater.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testExecuteWithWrappedUpdater()
    {
        final Sequence sequence = context.sequence("sequence-name");
        final EntityManager entityManager = context.mock(EntityManager.class);
        final PersistenceRequest req = context.mock(PersistenceRequest.class);
        final UpdateMapper wrappedUpdater = context.mock(UpdateMapper.class);

        UpdateMapper sut = new UpdateMapper(wrappedUpdater);
        sut.setEntityManager(entityManager);

        context.checking(new Expectations()
        {
            {
                oneOf(entityManager).flush();
                inSequence(sequence);

                oneOf(wrappedUpdater).execute(req);
                inSequence(sequence);
            }
        });

        assertTrue(sut.execute(req));
        context.assertIsSatisfied();
    }
}
