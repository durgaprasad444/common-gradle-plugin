/*
 * common-gradle-plugin
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
import org.apache.commons.lang.StringUtils
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.kt3k.gradle.plugin.CoverallsPlugin
import org.sonarqube.gradle.SonarQubeExtension
import org.sonarqube.gradle.SonarQubePlugin

import com.hierynomus.gradle.license.LicenseBasePlugin

import nl.javadude.gradle.plugins.license.LicenseExtension

abstract class Common implements Plugin<Project> {
    public static final PROPERTY_ARTIFACTORY_URL = 'artifactoryUrl'
    public static final PROPERTY_ARTIFACTORY_REPO = 'artifactoryRepo'
    public static final PROPERTY_ARTIFACTORY_SNAPSHOT_REPO = 'artifactorySnapshotRepo'
    public static final PROPERTY_ARTIFACTORY_RELEASE_REPO = 'artifactoryReleaseRepo'

    public static final PROPERTY_ARTIFACTORY_DEPLOYER_USERNAME = 'artifactoryDeployerUsername'
    public static final PROPERTY_ARTIFACTORY_DEPLOYER_PASSWORD = 'artifactoryDeployerPassword'
    public static final ENVIRONMENT_VARIABLE_ARTIFACTORY_DEPLOYER_USERNAME = 'ARTIFACTORY_DEPLOYER_USER'
    public static final ENVIRONMENT_VARIABLE_ARTIFACTORY_DEPLOYER_PASSWORD = 'ARTIFACTORY_DEPLOYER_PASSWORD'

    public static final PROPERTY_SONATYPE_USERNAME = 'sonatypeUsername'
    public static final PROPERTY_SONATYPE_PASSWORD = 'sonatypePassword'
    public static final ENVIRONMENT_VARIABLE_SONATYPE_USERNAME = 'SONATYPE_USERNAME'
    public static final ENVIRONMENT_VARIABLE_SONATYPE_PASSWORD = 'SONATYPE_PASSWORD'

    public static final PROPERTY_SONAR_QUBE_LOGIN = 'sonarQubeLogin'
    public static final ENVIRONMENT_VARIABLE_SONAR_QUBE_LOGIN = 'SONAR_QUBE_LOGIN'

    void apply(Project project) {
        if (StringUtils.isBlank(project.version) || project.version == 'unspecified') {
            throw new GradleException('The version must be specified before applying this plugin.')
        }

        project.ext.isSnapshot = project.version.endsWith('-SNAPSHOT')

        //assume some reasonable defaults if the environment doesn't provide specific values
        project.ext.artifactoryUrl = project.findProperty(PROPERTY_ARTIFACTORY_URL)
        if (!project.ext.artifactoryUrl) {
            project.ext.artifactoryUrl = 'https://prd-eng-repo02.dc2.lan/artifactory'
        }
        project.ext.artifactoryRepo = project.findProperty(PROPERTY_ARTIFACTORY_REPO)
        if (!project.ext.artifactoryRepo) {
            project.ext.artifactoryRepo = 'bds-integrations-snapshot'
        }
        project.ext.artifactorySnapshotRepo = project.findProperty(PROPERTY_ARTIFACTORY_SNAPSHOT_REPO)
        if (!project.ext.artifactorySnapshotRepo) {
            project.ext.artifactorySnapshotRepo = 'bds-integrations-snapshot'
        }
        project.ext.artifactoryReleaseRepo = project.findProperty(PROPERTY_ARTIFACTORY_RELEASE_REPO)
        if (!project.ext.artifactoryReleaseRepo) {
            project.ext.artifactoryReleaseRepo = 'bds-integrations-release'
        }

        //but passwords have no reasonable defaults
        project.ext.artifactoryDeployerUsername = project.findProperty(PROPERTY_ARTIFACTORY_DEPLOYER_USERNAME)
        if (!project.ext.artifactoryDeployerUsername) {
            project.ext.artifactoryDeployerUsername = System.getenv(ENVIRONMENT_VARIABLE_ARTIFACTORY_DEPLOYER_USERNAME)
        }
        project.ext.artifactoryDeployerPassword = project.findProperty(PROPERTY_ARTIFACTORY_DEPLOYER_PASSWORD)
        if (!project.ext.artifactoryDeployerPassword) {
            project.ext.artifactoryDeployerPassword = System.getenv(ENVIRONMENT_VARIABLE_ARTIFACTORY_DEPLOYER_PASSWORD)
        }
        project.ext.sonatypeUsername = project.findProperty(PROPERTY_SONATYPE_USERNAME)
        if (!project.ext.sonatypeUsername) {
            project.ext.sonatypeUsername = System.getenv(ENVIRONMENT_VARIABLE_SONATYPE_USERNAME)
        }
        project.ext.sonatypePassword = project.findProperty(PROPERTY_SONATYPE_PASSWORD)
        if (!project.ext.sonatypePassword) {
            project.ext.sonatypePassword = System.getenv(ENVIRONMENT_VARIABLE_SONATYPE_PASSWORD)
        }
        project.ext.sonarQubeLogin = project.findProperty(PROPERTY_SONAR_QUBE_LOGIN)
        if (!project.ext.sonarQubeLogin) {
            project.ext.sonarQubeLogin = System.getenv(ENVIRONMENT_VARIABLE_SONAR_QUBE_LOGIN)
        }

        project.repositories {
            jcenter()
            mavenCentral()
            maven { url 'https://plugins.gradle.org/m2/' }
        }

        project.plugins.apply('java')
        project.plugins.apply('eclipse')
        project.plugins.apply('maven')
        project.plugins.apply('jacoco')
        project.plugins.apply(LicenseBasePlugin.class)
        project.plugins.apply(CoverallsPlugin.class)
        project.plugins.apply(ArtifactoryPlugin.class)
        project.plugins.apply(SonarQubePlugin.class)

        project.tasks.withType(JavaCompile) {
            options.encoding = 'UTF-8'
            if (project.hasProperty('jvmArgs')) {
                options.compilerArgs.addAll(project.jvmArgs.split(','))
            }
        }
        project.tasks.withType(GroovyCompile) {
            options.encoding = 'UTF-8'
            if (project.hasProperty('jvmArgs')) {
                options.compilerArgs.addAll(project.jvmArgs.split(','))
            }
        }

        project.group = 'com.blackducksoftware.integration'
        project.dependencies { testCompile 'junit:junit:4.12' }

        configureForJava(project)
        configureForLicense(project)
        configureForSonarQube(project)
    }

    public void configureForJava(Project project) {
        Task jarTask = project.tasks.getByName('jar')
        Task classesTask = project.tasks.getByName('classes')
        Task javadocTask = project.tasks.getByName('javadoc')
        Configuration archivesConfiguration = project.configurations.getByName('archives')
        JavaPluginConvention javaPluginConvention = project.convention.getPlugin(JavaPluginConvention.class)

        javaPluginConvention.sourceCompatibility = 1.8
        javaPluginConvention.targetCompatibility = 1.8

        Task sourcesJarTask = project.tasks.create(name: 'sourcesJar', type: Jar, dependsOn: classesTask) {
            classifier = 'sources'
            from javaPluginConvention.sourceSets.main.allSource
        }

        Task javadocJarTask = project.tasks.create(name: 'javadocJar', type: Jar, dependsOn: javadocTask) {
            classifier = 'javadoc'
            from javadocTask.destinationDir
        }

        if (JavaVersion.current().isJava8Compatible()) {
            project.tasks.withType(Javadoc) {
                options.addStringOption('Xdoclint:none', '-quiet')
            }
        }

        project.tasks.getByName('jacocoTestReport').reports {
            // coveralls plugin demands xml format
            xml.enabled = true
            html.enabled = true
        }

        project.artifacts.add('archives', jarTask)
        project.artifacts.add('archives', sourcesJarTask)
        project.artifacts.add('archives', javadocJarTask)
    }

    public void configureForLicense(Project project) {
        LicenseExtension licenseExtension = project.extensions.getByName('license')
        licenseExtension.headerURI = new URI('https://blackducksoftware.github.io/common-gradle-plugin/HEADER.txt')
        licenseExtension.ext.year = Calendar.getInstance().get(Calendar.YEAR)
        licenseExtension.ext.projectName = project.name
        licenseExtension.ignoreFailures = true
        licenseExtension.strictCheck = true
        licenseExtension.includes (['**/*.groovy', '**/*.java'])
        licenseExtension.excludes ([
            '/src/test/*.groovy',
            'src/test/*.java'
        ])

        //task to apply the header to all included files
        Task licenseFormatMainTask = project.tasks.getByName('licenseFormatMain')
        project.tasks.getByName('build').dependsOn(licenseFormatMainTask)
    }

    public void configureForSonarQube(Project project) {
        SonarQubeExtension sonarQubeExtension = project.extensions.getByName('sonarqube')
        sonarQubeExtension.properties {
            property 'sonar.host.url', 'https://sonarcloud.io'
            property 'sonar.organization', 'black-duck-software'
            property 'sonar.login', project.ext.sonarQubeLogin
        }
    }

    public void configureDefaultsForArtifactory(Project project, String artifactoryRepo) {
        configureDefaultsForArtifactory(project, artifactoryRepo, null)
    }

    public void configureDefaultsForArtifactory(Project project, String artifactoryRepo, Closure defaultsClosure) {
        ArtifactoryPluginConvention artifactoryPluginConvention = project.convention.plugins.get('artifactory')
        artifactoryPluginConvention.contextUrl = project.ext.artifactoryUrl
        artifactoryPluginConvention.publish {
            repository { repoKey = artifactoryRepo }
            username = project.ext.artifactoryDeployerUsername
            password = project.ext.artifactoryDeployerPassword
        }

        if (defaultsClosure != null) {
            artifactoryPluginConvention.publisherConfig.defaults(defaultsClosure)
        }

        project.tasks.getByName('artifactoryPublish').dependsOn { println "artifactoryPublish will attempt uploading ${project.name}:${project.version} to ${artifactoryRepo}" }
    }
}