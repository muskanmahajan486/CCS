package org.openremote.controllercommand.rest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openremote.devicediscovery.domain.DiscoveredDeviceAttrDTO;
import org.openremote.devicediscovery.domain.DiscoveredDeviceDTO;
import org.openremote.rest.GenericResourceResultWithErrorMessage;
import org.restlet.data.ChallengeScheme;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

public class ControllerCommandTests
{

  private static DiscoveredDeviceDTO deviceToUpdate;
  private static List<Long> addedDeviceOIDs;

  /**
   * Test: Add devices
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testAddDevices() throws Exception
  {
    ClientResource cr = new ClientResource("http://localhost:8080/dds/rest/discoveredDevices");
    cr.setChallengeResponse(ChallengeScheme.HTTP_BASIC, "test55", "test55");

    List<DiscoveredDeviceDTO> list = new ArrayList<DiscoveredDeviceDTO>();
    
    DiscoveredDeviceDTO a = new DiscoveredDeviceDTO();
    a.setModel("model1");
    a.setName("name1");
    a.setProtocol("zwave");
    a.setType("Switch");
    a.setUsed(false);
    List<DiscoveredDeviceAttrDTO> deviceAttrs = new ArrayList<DiscoveredDeviceAttrDTO>();
    DiscoveredDeviceAttrDTO b = new DiscoveredDeviceAttrDTO();
    b.setName("id");
    b.setValue("11");
    deviceAttrs.add(b);
    a.setDeviceAttrs(deviceAttrs);
    list.add(a);

    a = new DiscoveredDeviceDTO();
    a.setModel("model2");
    a.setName("name2");
    a.setProtocol("samsung");
    a.setType("TV");
    a.setUsed(false);
    deviceAttrs = new ArrayList<DiscoveredDeviceAttrDTO>();
    b = new DiscoveredDeviceAttrDTO();
    b.setName("ip");
    b.setValue("111.111.111.111");
    deviceAttrs.add(b);
    a.setDeviceAttrs(deviceAttrs);
    list.add(a);
    
    Representation rep = new JsonRepresentation(new JSONSerializer().exclude("*.class").deepSerialize(list));
    Representation result = cr.post(rep);

    GenericResourceResultWithErrorMessage res = new JSONDeserializer<GenericResourceResultWithErrorMessage>().use(null, GenericResourceResultWithErrorMessage.class).use("result", ArrayList.class).use("result.values", Long.class).deserialize(result.getText());
    addedDeviceOIDs = (List<Long>)res.getResult();
    Assert.assertEquals(2, ((List)res.getResult()).size());
    
  }
  
  /**
   * Test: Query for all discovered devices
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testQueryAllDevices() throws Exception
  {
    ClientResource cr = new ClientResource("http://localhost:8080/dds/rest/discoveredDevices");
    cr.setChallengeResponse(ChallengeScheme.HTTP_BASIC, "test55", "test55");
    Representation r = cr.get();
    String str = r.getText();
    GenericResourceResultWithErrorMessage res =new JSONDeserializer<GenericResourceResultWithErrorMessage>().use(null, GenericResourceResultWithErrorMessage.class).use("result", ArrayList.class).use("result.values", DiscoveredDeviceDTO.class).deserialize(str); 
    List<DiscoveredDeviceDTO> dtos = (List<DiscoveredDeviceDTO>)res.getResult(); 

    Assert.assertEquals(2, dtos.size());
  }

  /**
   * Test: Query for one device
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testQueryOneDevice() throws Exception
  {
    ClientResource cr = new ClientResource("http://localhost:8080/dds/rest/discoveredDevices?protocol=zwave");
    cr.setChallengeResponse(ChallengeScheme.HTTP_BASIC, "test55", "test55");
    Representation r = cr.get();
    String str = r.getText();
    GenericResourceResultWithErrorMessage res =new JSONDeserializer<GenericResourceResultWithErrorMessage>().use(null, GenericResourceResultWithErrorMessage.class).use("result", ArrayList.class).use("result.values", DiscoveredDeviceDTO.class).deserialize(str); 
    List<DiscoveredDeviceDTO> dtos = (List<DiscoveredDeviceDTO>)res.getResult(); 
    deviceToUpdate = dtos.get(0);
    Assert.assertEquals(1, dtos.size());
  }
  
  /**
   * Test: update one device
   */
  @Test
  public void testQueryUpdateDevice() throws Exception
  {
    ClientResource cr = new ClientResource("http://localhost:8080/dds/rest/discoveredDevices/" + deviceToUpdate.getOid());
    cr.setChallengeResponse(ChallengeScheme.HTTP_BASIC, "test55", "test55");

    deviceToUpdate.setUsed(true);
    Representation rep = new JsonRepresentation(new JSONSerializer().exclude("*.class").deepSerialize(deviceToUpdate));
    Representation result = cr.put(rep);
    String str = result.getText();
    GenericResourceResultWithErrorMessage res =new JSONDeserializer<GenericResourceResultWithErrorMessage>().use(null, GenericResourceResultWithErrorMessage.class).use("result", DiscoveredDeviceDTO.class).deserialize(str); 

    Assert.assertEquals(deviceToUpdate.getOid(), ((DiscoveredDeviceDTO)res.getResult()).getOid());
  }
  
  /**
   * Test: delete devices
   */
  @Test
  public void testDeleteDevices() throws Exception
  {
    for (Long oid: addedDeviceOIDs)
    {
      ClientResource cr = new ClientResource("http://localhost:8080/dds/rest/discoveredDevices/" + oid);
      cr.setChallengeResponse(ChallengeScheme.HTTP_BASIC, "test55", "test55");
      Representation result = cr.delete();
      String str = result.getText();
      GenericResourceResultWithErrorMessage res =new JSONDeserializer<GenericResourceResultWithErrorMessage>().use(null, GenericResourceResultWithErrorMessage.class).use("result", String.class).deserialize(str); 
      Assert.assertEquals(null, res.getErrorMessage());
    }

  }
}
