/*
*Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.registry.es.notifications;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wink.client.ClientResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.governance.api.util.GovernanceArtifactConfiguration;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.governance.generic.stub.ManageGenericArtifactServiceRegistryExceptionException;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.registry.es.utils.GregESTestBaseTest;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.ui.deployment.ComponentBuilder;
import org.wso2.carbon.ui.deployment.beans.Component;
import org.wso2.carbon.ui.deployment.beans.Menu;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.greg.integration.common.clients.ManageGenericArtifactAdminServiceClient;
import org.wso2.greg.integration.common.clients.ResourceAdminServiceClient;
import org.wso2.greg.integration.common.utils.GREGIntegrationBaseTest;
import org.wso2.greg.integration.common.utils.GenericRestClient;
import org.wso2.greg.integration.common.utils.RegistryProviderUtil;

import javax.activation.DataHandler;
import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static org.testng.Assert.assertNotNull;

/**
 * This class test subscription & notification for custom rxt type at store.
 */

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
public class CustomRXTStoreNotificationTestCase extends GregESTestBaseTest {

    private static final Log log = LogFactory.getLog(CustomRXTStoreNotificationTestCase.class);

    private TestUserMode userMode;
    String jSessionIdPublisher;
    String jSessionIdStore;
    String assetId;
    String cookieHeaderPublisher;
    String cookieHeaderStore;
    GenericRestClient genericRestClient;
    Map<String, String> queryParamMap;
    Map<String, String> headerMap;
    String publisherUrl;
    String storeUrl;
    String resourcePath;
    private ResourceAdminServiceClient resourceAdminServiceClient;

    @Factory(dataProvider = "userModeProvider")
    public CustomRXTStoreNotificationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(userMode);
        String session = getSessionCookie();
        genericRestClient = new GenericRestClient();
        queryParamMap = new HashMap<String, String>();
        headerMap = new HashMap<String, String>();
        resourcePath =
                FrameworkPathUtil.getSystemResourceLocation() + "artifacts" + File.separator + "GREG" + File.separator;
        publisherUrl = publisherContext.getContextUrls().getSecureServiceUrl().replace("services", "publisher/apis");
        storeUrl = storeContext.getContextUrls().getSecureServiceUrl().replace("services", "store/apis");
        resourceAdminServiceClient = new ResourceAdminServiceClient(backendURL, session);
        addCustomRxt();
        setTestEnvironment();
    }

    @Test(groups = { "wso2.greg",
            "wso2.greg.es" }, description = "Adding subscription to custom asset on LC state change")
    public void addSubscriptionToLcStateChange() throws JSONException, IOException {
        JSONObject dataObject = new JSONObject();
        dataObject.put("notificationType", "StoreLifeCycleStateChanged");
        dataObject.put("notificationMethod", "work");

        ClientResponse response = genericRestClient
                .geneticRestRequestPost(publisherUrl + "/subscription/applications/" + assetId, MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_JSON, dataObject.toString(), queryParamMap, headerMap, cookieHeaderStore);

        String payLoad = response.getEntity(String.class);
        payLoad = payLoad.substring(payLoad.indexOf('{'));
        JSONObject obj = new JSONObject(payLoad);
        assertNotNull(obj.get("id").toString(),
                "Response payload is not the in the correct format" + response.getEntity(String.class));

        genericRestClient.geneticRestRequestPost(publisherUrl + "/assets/" + assetId + "/state",
                MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON,
                "nextState=Testing&comment=Completed", queryParamMap, headerMap, cookieHeaderPublisher);
        // TODO - Since notification not appearing in the store
    }

    @Test(groups = { "wso2.greg",
            "wso2.greg.es" }, description = "Adding subscription to custom asset on resource update")
    public void addSubscriptionToResourceUpdate() throws JSONException, IOException {
        JSONObject dataObject = new JSONObject();
        dataObject.put("notificationType", "StoreResourceUpdated");
        dataObject.put("notificationMethod", "work");

        ClientResponse response = genericRestClient
                .geneticRestRequestPost(storeUrl + "/subscription/applications/" + assetId, MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_JSON, dataObject.toString(), queryParamMap, headerMap, cookieHeaderStore);

        String payLoad = response.getEntity(String.class);
        payLoad = payLoad.substring(payLoad.indexOf('{'));
        JSONObject obj = new JSONObject(payLoad);
        assertNotNull(obj.get("id").toString(),
                "Response payload is not the in the correct format" + response.getEntity(String.class));

        queryParamMap.put("type", "applications");
        String dataBody = readFile(resourcePath + "json" + File.separator + "PublisherCustomResourceUpdate.json");
        genericRestClient.geneticRestRequestPost(publisherUrl + "/assets/" + assetId, MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON, dataBody, queryParamMap, headerMap, cookieHeaderPublisher);
        // TODO - Since notification not appearing in the store
    }

    @Test(groups = { "wso2.greg",
            "wso2.greg.es" }, description = "Adding wrong subscription method to check the error message")
    public void addWrongSubscriptionMethod() throws JSONException, IOException {

        JSONObject dataObject = new JSONObject();
        dataObject.put("notificationType", "StoreResourceUpdated");
        dataObject.put("notificationMethod", "test");

        ClientResponse response = genericRestClient
                .geneticRestRequestPost(storeUrl + "/subscription/applications/" + assetId,
                        MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, dataObject.toString(), queryParamMap,
                        headerMap, cookieHeaderStore);

        String payLoad = response.getEntity(String.class);
        payLoad = payLoad.substring(payLoad.indexOf('{'));
        JSONObject obj = new JSONObject(payLoad);
        assertNotNull(obj.get("error").toString(),
                "Error message is not contained in the response for notification method \"test\"" + response
                        .getEntity(String.class));
    }

    private void addCustomRxt()
            throws Exception {
        String filePath = getTestArtifactLocation() + "artifacts" + File.separator +
                "GREG" + File.separator + "rxt" + File.separator + "application.rxt";
        DataHandler dh = new DataHandler(new URL("file:///" + filePath));





        ManageGenericArtifactAdminServiceClient manageGenericArtifactAdminServiceClient =new ManageGenericArtifactAdminServiceClient(backendURL, getSessionCookie());

        final InputStream in = dh.getInputStream();
        byte[] byteArray=org.apache.commons.io.IOUtils.toByteArray(in);



        resourceAdminServiceClient.addResource(
                "/_system/governance/repository/components/org.wso2.carbon.governance/types/applications.rxt",
                "application/vnd.wso2.registry-ext-type+xml", "desc", dh);

    }

    private void deleteCustomAsset() throws JSONException {
        genericRestClient.geneticRestRequestDelete(publisherUrl + "/assets/" + assetId, MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON, queryParamMap, headerMap, cookieHeaderPublisher);

    }

    private void deleteCustomRxt() throws Exception {
        String session = getSessionCookie();
        resourceAdminServiceClient = new ResourceAdminServiceClient(backendURL, session);
        resourceAdminServiceClient.deleteResource(
                "/_system/governance/repository/components/org.wso2.carbon.governance/types/applications.rxt");
    }

    /**
     * Need to refresh the landing page to deploy the new rxt in publisher
     */
    private void refreshPublisherLandingPage() {
        Map<String, String> queryParamMap = new HashMap<>();
        String landingUrl = publisherUrl.replace("apis", "pages/gc-landing");
        genericRestClient.geneticRestRequestGet(landingUrl, queryParamMap, headerMap, cookieHeaderPublisher);
    }

    private void setTestEnvironment() throws JSONException, IOException, XPathExpressionException {
        // Authenticate Publisher
        ClientResponse response = authenticate(publisherUrl, genericRestClient,
                automationContext.getSuperTenant().getTenantAdmin().getUserName(),
                automationContext.getSuperTenant().getTenantAdmin().getPassword());
        JSONObject obj = new JSONObject(response.getEntity(String.class));
        jSessionIdPublisher = obj.getJSONObject("data").getString("sessionId");
        cookieHeaderPublisher = "JSESSIONID=" + jSessionIdPublisher;
        //refresh the publisher landing page to deploy new rxt type
        refreshPublisherLandingPage();

        // Authenticate Store
        ClientResponse responseStore = authenticate(storeUrl, genericRestClient,
                automationContext.getSuperTenant().getTenantAdmin().getUserName(),
                automationContext.getSuperTenant().getTenantAdmin().getPassword());
        obj = new JSONObject(responseStore.getEntity(String.class));
        jSessionIdStore = obj.getJSONObject("data").getString("sessionId");
        cookieHeaderStore = "JSESSIONID=" + jSessionIdStore;

        //Create rest service
        queryParamMap.put("type", "applications");
        String dataBody = readFile(resourcePath + "json" + File.separator + "publisherPublishCustomResource.json");
        ClientResponse createResponse = genericRestClient
                .geneticRestRequestPost(publisherUrl + "/assets", MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_JSON, dataBody, queryParamMap, headerMap, cookieHeaderPublisher);
        JSONObject createObj = new JSONObject(createResponse.getEntity(String.class));
        assetId = createObj.get("id").toString();
    }

    @AfterClass(alwaysRun = true)
    public void clean() throws Exception {
        deleteCustomAsset();
        deleteCustomRxt();
    }

    @DataProvider
    private static TestUserMode[][] userModeProvider() {
        return new TestUserMode[][]{
                new TestUserMode[]{TestUserMode.SUPER_TENANT_ADMIN}
                //                new TestUserMode[]{TestUserMode.TENANT_USER},
        };
    }

    public void buildMenuItems(String cookie, String s, String s1, String s2) throws Exception{
            try {
                WSRegistryServiceClient wsRegistryServiceClient = new RegistryProviderUtil().getWSRegistry(publisherContext);

                Registry governance = new RegistryProviderUtil().getGovernanceRegistry(wsRegistryServiceClient, publisherContext);
                GovernanceUtils.loadGovernanceArtifacts((UserRegistry) governance,
                        GovernanceUtils.findGovernanceArtifactConfigurations(governance));
                List<GovernanceArtifactConfiguration> configurations =
                        GovernanceUtils.findGovernanceArtifactConfigurations(wsRegistryServiceClient);
                Map<String, String> customAddUIMap = new LinkedHashMap<String, String>();
                Map<String, String> customViewUIMap = new LinkedHashMap<String, String>();
                List<Menu> userCustomMenuItemsList = new LinkedList<Menu>();
                String configurationPath = RegistryConstants.CONFIG_REGISTRY_BASE_PATH +
                        RegistryConstants.GOVERNANCE_COMPONENT_PATH +
                        "/configuration/";
                wsRegistryServiceClient.delete(configurationPath);
                for (GovernanceArtifactConfiguration configuration : configurations) {
                    Component component = new Component();
                    OMElement uiConfigurations = configuration.getUIConfigurations();
                    String key = configuration.getKey();

                    String layoutStoragePath = configurationPath
                            + key;
                    RealmService realmService = wsRegistryServiceClient.getRegistryContext().getRealmService();
                    //if (realmService.getTenantUserRealm(realmService.getTenantManager().getTenantId(s1))
                     //       .getAuthorizationManager().isUserAuthorized(s, configurationPath, ActionConstants.PUT)
                     //       || wsRegistryServiceClient.resourceExists(layoutStoragePath)) {
                        List<Menu> menuList = component.getMenusList();
                        if (uiConfigurations != null) {
                            ComponentBuilder
                                    .processMenus("artifactType", uiConfigurations, component);
                            ComponentBuilder.processCustomUIs(uiConfigurations, component);
                        }
                        userCustomMenuItemsList.addAll(menuList);
                        customAddUIMap.putAll(component.getCustomAddUIMap());
                        Map<String, String> viewUIMap =
                                component.getCustomViewUIMap();
                        if (viewUIMap.isEmpty()) {
                            // if no custom UI definitions were present, define the default.
                            buildViewUI(configuration, viewUIMap, key);
                        }
                        customViewUIMap.putAll(viewUIMap);
                        OMElement layout = configuration.getContentDefinition();
                        if (layout != null && !wsRegistryServiceClient.resourceExists(layoutStoragePath)) {
                            Resource resource = wsRegistryServiceClient.newResource();
                            resource.setContent(RegistryUtils.encodeString(layout.toString()));
                            resource.setMediaType("application/xml");
                            wsRegistryServiceClient.put(layoutStoragePath, resource);
                        }
                    //}
                }
            } catch (RegistryException e) {
                log.error("unable to create connection to registry");
            } catch (org.wso2.carbon.user.api.UserStoreException e) {
                log.error("unable to realm service");
            }
        }

    private static void buildViewUI(GovernanceArtifactConfiguration configuration,
                                    Map<String, String> viewUIMap, String key) {
        String singularLabel = configuration.getSingularLabel();
        String pluralLabel = configuration.getPluralLabel();

        String lifecycleAttribute = key + "Lifecycle_lifecycleName";

        if (singularLabel == null || pluralLabel == null) {
            log.error("The singular label and plural label have not " +
                    "been defined for the artifact type: " + key);
        } else {
            String contentURL = configuration.getContentURL();
            if (contentURL != null) {
                if (!contentURL.toLowerCase().equals("default")) {
                    viewUIMap.put(configuration.getMediaType(), contentURL);
                }
            } else {
                String path = "../generic/edit_ajaxprocessor.jsp?hideEditView=true&key=" + key +
                        "&lifecycleAttribute=" + lifecycleAttribute +"&add_edit_breadcrumb=" +
                        singularLabel + "&add_edit_region=region3&add_edit_item=governance_add_" +
                        key + "_menu&breadcrumb=" + singularLabel;
                viewUIMap.put(configuration.getMediaType(), path);
            }
        }
    }
}
