package de.kddc.mybench.clients

import de.kddc.mybench.clients.OpenStreetMapClientProtocol.OpenStreetMapNode
import de.kddc.mybench.utils.BBox
import de.kddc.mybench.{ServiceComponents, ServiceTest}

class OpenStreetMapClientTest extends ServiceTest with ServiceComponents {
  "works" in {
    val bbox = BBox(53.91798871241739,9.499955177307129,53.94972163975539,9.56033706665039)
    val benches = openStreetMapClient.findNodes(bbox).futureValue
    benches should not be empty
    every (benches) should matchPattern { case OpenStreetMapNode(_, "node", lat, lng) if lat >= bbox.south && lat <= bbox.north && lng >= bbox.west && lng <= bbox.east => }
  }
}
