/*
 * common-gradle-plugin
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.plugins.signing.SigningExtension
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention

import io.codearte.gradle.nexus.NexusStagingExtension
import io.codearte.gradle.nexus.NexusStagingPlugin

/**
 * This plugin is intended for common libraries. They will be published to
 * maven central and artifactory, using the version (SNAPSHOT or release) to
 * determine the appropriate destination for each.
 */
class LibraryPlugin extends Common {
    void apply(Project project) {
        super.apply(project)

        project.plugins.apply('signing')
        project.plugins.apply(NexusStagingPlugin.class)

        configureForArtifactoryUpload(project)
        configureForMavenCentralUpload(project)
        configureForNexusStagingAutoRelease(project)
    }

    private void configureForArtifactoryUpload(Project project) {
        String artifactoryRepo = project.findProperty('artifactorySnapshotRepo')
        if (!project.version.endsWith('-SNAPSHOT')) {
            artifactoryRepo = project.findProperty('artifactoryReleaseRepo')
        }

        ArtifactoryPluginConvention artifactoryPluginConvention = project.convention.plugins.get('artifactory')
        artifactoryPluginConvention.contextUrl = project.findProperty('artifactoryUrl')
        artifactoryPluginConvention.publish {
            repository {
                repoKey = artifactoryRepo
                username = project.findProperty('artifactoryDeployerUsername')
                password = project.findProperty('artifactoryDeployerPassword')
            }
            defaults { publishConfigs ('archives') }
        }
    }

    private void configureForMavenCentralUpload(Project project) {
        NexusStagingExtension nexusStagingExtension = project.extensions.getByName('nexusStaging')
        nexusStagingExtension.packageGroup = 'com.blackducksoftware'

        Configuration archivesConfiguration = project.getConfigurations().getByName('archives')
        SigningExtension signingExtension = project.extensions.getByName('signing')
        signingExtension.required {
            project.gradle.taskGraph.hasTask('uploadArchives')
        }
        signingExtension.sign(archivesConfiguration)

        String sonatypeUsername = project.findProperty('sonatypeUsername')
        String sonatypePassword = project.findProperty('sonatypePassword')
        String rootProjectName = project.getRootProject().getName()
        project.uploadArchives {
            repositories {
                mavenDeployer {
                    beforeDeployment { MavenDeployment deployment ->
                        signingExtension.signPom(deployment)
                    }
                    repository(url: 'https://oss.sonatype.org/service/local/staging/deploy/maven2/') {
                        authentication(userName: sonatypeUsername, password: sonatypePassword)
                    }
                    snapshotRepository(url: 'https://oss.sonatype.org/content/repositories/snapshots/') {
                        authentication(userName: sonatypeUsername, password: sonatypePassword)
                    }
                    pom.project {
                        name project.rootProject.name
                        description project.rootProject.description
                        url "https://www.github.com/blackducksoftware/${rootProjectName}"
                        packaging 'jar'
                        scm {
                            connection "scm:git:git://github.com/blackducksoftware/${rootProjectName}.git"
                            developerConnection "scm:git:git@github.com:blackducksoftware/${rootProjectName}.git"
                            url "https://www.github.com/blackducksoftware/${rootProjectName}"
                        }
                        licenses {
                            license {
                                name 'Apache License 2.0'
                                url 'http://www.apache.org/licenses/LICENSE-2.0'
                            }
                        }
                        developers {
                            developer {
                                id 'blackduckoss'
                                name 'Black Duck OSS'
                                email 'bdsoss@blackducksoftware.com'
                                organization 'Black Duck Software, Inc.'
                                organizationUrl 'http://www.blackducksoftware.com'
                                roles { role 'developer' }
                                timezone 'America/New_York'
                            }
                        }
                    }
                }
            }
        }
    }

    private void configureForNexusStagingAutoRelease(Project project) {
        project.getTasks().getByName('closeRepository').onlyIf {
            !project.version.endsWith('-SNAPSHOT')
        }
        project.getTasks().getByName('releaseRepository').onlyIf {
            !project.version.endsWith('-SNAPSHOT')
        }
    }
}