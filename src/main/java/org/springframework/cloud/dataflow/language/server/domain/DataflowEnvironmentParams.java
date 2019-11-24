/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.language.server.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

public class DataflowEnvironmentParams {

    private List<Environment> environments = new ArrayList<>();
    private String defaultEnvironment;
    private Boolean trustssl;

    public List<Environment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<Environment> environments) {
        this.environments = environments;
    }

    public String getDefaultEnvironment() {
        return defaultEnvironment;
    }

    public void setDefaultEnvironment(String defaultEnvironment) {
        this.defaultEnvironment = defaultEnvironment;
    }

    public Boolean getTrustssl() {
        return trustssl;
    }

    public void setTrustssl(Boolean trustssl) {
        this.trustssl = trustssl;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((defaultEnvironment == null) ? 0 : defaultEnvironment.hashCode());
        result = prime * result + ((environments == null) ? 0 : environments.hashCode());
        result = prime * result + ((trustssl == null) ? 0 : trustssl.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DataflowEnvironmentParams other = (DataflowEnvironmentParams) obj;
        if (defaultEnvironment == null) {
            if (other.defaultEnvironment != null)
                return false;
        } else if (!defaultEnvironment.equals(other.defaultEnvironment))
            return false;
        if (environments == null) {
            if (other.environments != null)
                return false;
        } else if (!environments.equals(other.environments))
            return false;
        if (trustssl == null) {
            if (other.trustssl != null)
                return false;
        } else if (!trustssl.equals(other.trustssl))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DataflowEnvironmentParams [defaultEnvironment=" + defaultEnvironment + ", environments=" + environments
                + ", trustssl=" + trustssl + "]";
    }

    public static class Environment {

        private String url;
        private String name;
        private Credentials credentials = new Credentials();

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Credentials getCredentials() {
            return credentials;
        }

        public void setCredentials(Credentials credentials) {
            this.credentials = credentials;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((url == null) ? 0 : url.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((credentials == null) ? 0 : credentials.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Environment other = (Environment) obj;
            if (url == null) {
                if (other.url != null) {
                    return false;
                }
            } else if (!url.equals(other.url)) {
                return false;
            }
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (credentials == null) {
                if (other.credentials != null) {
                    return false;
                }
            } else if (!credentials.equals(other.credentials)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "DataflowEnvironmentParam [url=" + url + ", name=" + name + ", username=" + credentials.getUsername()
                    + ", password=" + (StringUtils.hasText(credentials.getPassword()) ? "********" : "") + "]";
        }
    }

    public static class Credentials {

        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((username == null) ? 0 : username.hashCode());
            result = prime * result + ((password == null) ? 0 : password.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Credentials other = (Credentials) obj;
            if (username == null) {
                if (other.username != null)
                    return false;
            } else if (!username.equals(other.username)) {
                return false;
            }
            if (password == null) {
                if (other.password != null)
                    return false;
            } else if (!password.equals(other.password)) {
                return false;
            }
            return true;
        }
    }
}
