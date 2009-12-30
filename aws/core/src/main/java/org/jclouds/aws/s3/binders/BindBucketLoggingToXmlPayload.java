/**
 *
 * Copyright (C) 2009 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 */
package org.jclouds.aws.s3.binders;

import java.util.Properties;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.jclouds.aws.s3.domain.BucketLogging;
import org.jclouds.aws.s3.domain.AccessControlList.CanonicalUserGrantee;
import org.jclouds.aws.s3.domain.AccessControlList.EmailAddressGrantee;
import org.jclouds.aws.s3.domain.AccessControlList.Grant;
import org.jclouds.aws.s3.domain.AccessControlList.GroupGrantee;
import org.jclouds.aws.s3.reference.S3Constants;
import org.jclouds.http.HttpRequest;
import org.jclouds.rest.Binder;
import org.jclouds.util.Utils;

import com.jamesmurty.utils.XMLBuilder;

/**
 * 
 * @author Adrian Cole
 */
public class BindBucketLoggingToXmlPayload implements Binder {

   public void bindToRequest(HttpRequest request, Object payload) {
      BucketLogging from = (BucketLogging) payload;
      Properties outputProperties = new Properties();
      outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
      try {
         String stringPayload = generateBuilder(from).asString(outputProperties);
         request.getHeaders().put(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML);
         request.getHeaders().put(HttpHeaders.CONTENT_LENGTH, stringPayload.getBytes().length + "");
         request.setPayload(stringPayload);
      } catch (Exception e) {
         Utils.rethrowIfRuntime(e);
         throw new RuntimeException("error transforming bucketLogging: " + from, e);
      }
   }

   protected XMLBuilder generateBuilder(BucketLogging bucketLogging)
            throws ParserConfigurationException, FactoryConfigurationError {
      XMLBuilder rootBuilder = XMLBuilder.create("BucketLoggingStatus").attr("xmlns",
               S3Constants.S3_REST_API_XML_NAMESPACE).e("LoggingEnabled");
      rootBuilder.e("TargetBucket").t(bucketLogging.getTargetBucket());
      rootBuilder.e("TargetPrefix").t(bucketLogging.getTargetPrefix());
      XMLBuilder grantsBuilder = rootBuilder.elem("TargetGrants");
      for (Grant grant : bucketLogging.getTargetGrants()) {
         XMLBuilder grantBuilder = grantsBuilder.elem("Grant");
         XMLBuilder granteeBuilder = grantBuilder.elem("Grantee").attr("xmlns:xsi",
                  "http://www.w3.org/2001/XMLSchema-instance");

         if (grant.getGrantee() instanceof GroupGrantee) {
            granteeBuilder.attr("xsi:type", "Group").elem("URI").text(
                     grant.getGrantee().getIdentifier());
         } else if (grant.getGrantee() instanceof CanonicalUserGrantee) {
            CanonicalUserGrantee grantee = (CanonicalUserGrantee) grant.getGrantee();
            granteeBuilder.attr("xsi:type", "CanonicalUser").elem("ID").text(
                     grantee.getIdentifier()).up();
            if (grantee.getDisplayName() != null) {
               granteeBuilder.elem("DisplayName").text(grantee.getDisplayName());
            }
         } else if (grant.getGrantee() instanceof EmailAddressGrantee) {
            granteeBuilder.attr("xsi:type", "AmazonCustomerByEmail").elem("EmailAddress").text(
                     grant.getGrantee().getIdentifier());
         }
         grantBuilder.elem("Permission").text(grant.getPermission().toString());
      }
      return grantsBuilder;
   }

}
