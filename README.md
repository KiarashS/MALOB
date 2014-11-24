MALOB
=====

MALOB is a SDN load balancer application for the Floodlight controller.

Different applications have different requirements. A web browsing application requires short response times, whereas for a large file transfer, high throughput is preferable. For that reason, different service requests may benefit from different load balancing algorithms. In light of that, we created MALOB a Multi-Algorithm Load Balancer that has the capability to adapt to the different types of requests. In the design of MALOB we considered three broad types of requests: One type of request that prefers short response time, hence the load balancer should choose the path with the lowest latency (Web browsing applications), one that praises the path with the higher throughput (File transfer applications) and finally, one that requires a significant amount of processing (genome’s treatment applications). The first is used when clients request relatively small data with high intensity but requiring an insignificant processing. HTTP requests are an example of such: the first GET request must be returned as quickest as possible, and the pages are very small, making latency the prime factor to consider. The second one is a request for larger amount of data. Downloading files using protocols such as FTP are an example of this type of request where, the throughput a path can offer is the variable with highest impact on the download time. In the last application we consider, requests requires a significant processing For these applications, path latency or throughput do not have a great impact, choosing the server with highest available CPU is the most important factor.

Considering this, upon receiving a new request MALOB analyses the destination port of the request, checks its services table and decides which algorithm fits better that type of request. The service tables that currently only has two entries(HTTP on port 80 and FTP on port 20), can be extended using the REST API:

curl –x POST –d '{"service_name":" BioApp ","algorithm":"3","port":"6789"}' http:///quantum/v1.0/services/

Algorithm - 1: Shortest Latency-Path Server (SL-PS): chooses the server whose path between itself and the client offers the lowest latency.

Algorithm - 2: Highest Throughput-Path Server (HT-PS): chooses the server whose path between itself and the client has the best throughput.

Algorithm - 3: Least CPU usage (CPU): in this algorithm, the server is with lowest CPU usage is chosen.
