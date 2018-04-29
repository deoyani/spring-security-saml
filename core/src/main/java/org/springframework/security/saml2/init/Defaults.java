/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.springframework.security.saml2.init;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.springframework.security.saml2.authentication.AuthenticationRequest;
import org.springframework.security.saml2.authentication.NameIDPolicy;
import org.springframework.security.saml2.metadata.Binding;
import org.springframework.security.saml2.metadata.Endpoint;
import org.springframework.security.saml2.metadata.IdentityProvider;
import org.springframework.security.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml2.metadata.NameID;
import org.springframework.security.saml2.metadata.ServiceProvider;
import org.springframework.security.saml2.metadata.ServiceProviderMetadata;
import org.springframework.security.saml2.signature.AlgorithmMethod;
import org.springframework.security.saml2.signature.DigestMethod;
import org.springframework.security.saml2.xml.SimpleKey;

import static org.springframework.security.saml2.authentication.RequestedAuthenticationContext.exact;
import static org.springframework.security.saml2.init.SpringSecuritySaml.getInstance;
import static org.springframework.security.saml2.signature.AlgorithmMethod.RSA_SHA1;
import static org.springframework.security.saml2.signature.DigestMethod.SHA1;

public class Defaults {

    public static AlgorithmMethod DEFAULT_SIGN_ALGORITHM = RSA_SHA1;
    public static DigestMethod DEFAULT_SIGN_DIGEST = SHA1;

    public static ServiceProviderMetadata serviceProviderMetadata(String baseUrl,
                                                                  List<SimpleKey> keys,
                                                                  SimpleKey signingKey) {
        return new ServiceProviderMetadata()
            .setEntityId(baseUrl)
            .setId(UUID.randomUUID().toString())
            .setSigningKey(signingKey, DEFAULT_SIGN_ALGORITHM, DEFAULT_SIGN_DIGEST)
            .setKeys(keys)
            .setProviders(
                Arrays.asList(
                    new ServiceProvider()
                        .setWantAssertionsSigned(true)
                        .setAuthnRequestsSigned(signingKey!=null)
                        .setAssertionConsumerService(
                            Arrays.asList(
                                getInstance().init().getEndpoint(baseUrl, "saml/sp/SSO", Binding.POST, 0, true),
                                getInstance().init().getEndpoint(baseUrl,"saml/sp/SSO", Binding.REDIRECT, 1, false)
                            )
                        )
                        .setNameIDs(Arrays.asList(NameID.PERSISTENT, NameID.EMAIL))
                        .setKeys(keys)
                        .setSingleLogoutService(
                            Arrays.asList(
                                getInstance().init().getEndpoint(baseUrl,"saml/sp/logout", Binding.REDIRECT, 0, true)
                            )
                        )
                )
            );
    }

    public static IdentityProviderMetadata identityProviderMetadata(String baseUrl,
                                                                    List<SimpleKey> keys,
                                                                    SimpleKey signingKey) {
        return new IdentityProviderMetadata()
            .setEntityId(baseUrl)
            .setId(UUID.randomUUID().toString())
            .setSigningKey(signingKey, DEFAULT_SIGN_ALGORITHM, DEFAULT_SIGN_DIGEST)
            .setKeys(keys)
            .setProviders(
                Arrays.asList(
                    new IdentityProvider()
                        .setWantAuthnRequestsSigned(true)
                        .setSingleSignOnService(
                            Arrays.asList(
                                getInstance().init().getEndpoint(baseUrl,"saml/idp/SSO", Binding.POST, 0, true),
                                getInstance().init().getEndpoint(baseUrl,"saml/idp/SSO", Binding.REDIRECT, 1, false)
                            )
                        )
                        .setNameIDs(Arrays.asList(NameID.PERSISTENT,NameID.EMAIL))
                        .setKeys(keys)
                        .setSingleLogoutService(
                            Arrays.asList(
                                getInstance().init().getEndpoint(baseUrl,"saml/idp/logout", Binding.REDIRECT, 0, true)
                            )
                        )
                )
            );

    }

    public static AuthenticationRequest authenticationRequest(
        ServiceProviderMetadata sp,
        IdentityProviderMetadata idp) {

        AuthenticationRequest request = new AuthenticationRequest()
            .setId(UUID.randomUUID().toString())
            .setIssueInstant(new DateTime(System.currentTimeMillis()))
            .setForceAuth(Boolean.FALSE)
            .setPassive(Boolean.FALSE)
            .setBinding(Binding.POST)
            .setAssertionConsumerService(getACSFromSp(sp))
            .setIssuer(sp.getEntityId())
            .setRequestedAuthenticationContext(exact)
            .setDestination(idp.getIdentityProvider().getSingleSignOnService().get(0));
        if (sp.getServiceProvider().isAuthnRequestsSigned() ) {
            request.setSigningKey(sp.getSigningKey(), sp.getAlgorithm(), sp.getDigest());
        }
        NameIDPolicy policy;
        if (idp.getDefaultNameId()!=null) {
            policy = new NameIDPolicy(
                idp.getDefaultNameId(),
                sp.getEntityAlias(),
                true
            );
        } else {
            policy = new NameIDPolicy(
                idp.getIdentityProvider().getNameIDs().get(0),
                sp.getEntityAlias(),
                true
            );
        }
        request.setNameIDPolicy(policy);
        return request;
    }

    private static Endpoint getACSFromSp(ServiceProviderMetadata sp) {
        Endpoint endpoint = sp.getServiceProvider().getAssertionConsumerService().get(0);
        for (Endpoint e : sp.getServiceProvider().getAssertionConsumerService()) {
            if (e.isDefault()) {
                endpoint = e;
            }
        }
        return endpoint;
    }


}
