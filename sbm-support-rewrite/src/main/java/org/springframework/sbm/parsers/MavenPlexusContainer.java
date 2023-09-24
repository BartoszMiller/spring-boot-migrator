/*
 * Copyright 2021 - 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.sbm.parsers;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.graph.GraphBuilder;
import org.codehaus.plexus.*;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URL;

@Slf4j
class MavenPlexusContainer {

    private PlexusContainer plexusContainer;

    public MavenPlexusContainer() {

    }

    @PostConstruct
    public void postConstruct() {
        plexusContainer = create();
    }

    public GraphBuilder lookup(Class<GraphBuilder> aClass) {
        try {
            return plexusContainer.lookup(aClass);
        } catch (ComponentLookupException e) {
            throw new RuntimeException(e);
        }
    }

    @Deprecated
    public PlexusContainer get() {
        return plexusContainer;
    }

    public PlexusContainer create() {
        try {
            ClassLoader parent = null;
            boolean isContainerAutoWiring = false;
            String containerClassPathScanning = PlexusConstants.SCANNING_ON;
            String containerComponentVisibility = PlexusConstants.GLOBAL_VISIBILITY;
            URL overridingComponentsXml = getClass().getClassLoader().getResource("META-INF/**/components.xml");

            ContainerConfiguration configuration = new DefaultContainerConfiguration();
            configuration.setAutoWiring(isContainerAutoWiring)
                    .setClassPathScanning(containerClassPathScanning)
                    .setComponentVisibility(containerComponentVisibility)
                    .setContainerConfigurationURL(overridingComponentsXml);

            // inspired from https://github.com/jenkinsci/lib-jenkins-maven-embedder/blob/master/src/main/java/hudson/maven/MavenEmbedderUtils.java#L141
            ClassWorld classWorld = new ClassWorld();

            log.debug("ClassLoader for Plexus ClassRealm: " + PlexusContainer.class.getClassLoader().getClass().getName());
            ClassRealm classRealm = new ClassRealm(classWorld, "maven", PlexusContainer.class.getClassLoader());
            ClassLoader effectiveParent = parent == null ? Thread.currentThread().getContextClassLoader() : parent;
            log.debug("Effective parent: " + effectiveParent.getClass().getName());
            ClassRealm parentRealm = new ClassRealm(classWorld, "maven-parent", effectiveParent);
            classRealm.setParentRealm(parentRealm);
            configuration.setRealm(classRealm);
            configuration.setClassWorld(classWorld);
            return new DefaultPlexusContainer(configuration);
        } catch (PlexusContainerException e) {
            throw new RuntimeException(e);
        }
    }
}
