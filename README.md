# Activiti Job Executor EE (Apache License 2.0)

This project contains a enterprise ready job executor for [Activiti](http://activiti.org).  The default job executor in Activiti uses a self managed Thread Pool, which is not sufficient when running Activiti on JEE compliant application servers. Also the hard coupling between process engine and job acquisition does not fit well into enterprise scenarios.

## Scope of this Project:
* Decouple Job Acquisition from Process Engines
    - Configure Acquition Workers
        + maxJobsPerAcquisition
        + lockTimeInMillis
        + waitTimeInMillis
    - Assign Process Engines to Acquisition Workers (Multiple Engines can be assigned to one acquisition worker)
* Job Executor EE implementation using the Java Connector Architecture (JCA)
    - JEE 1.5 and 1.6 compliant.
    - Obtain threads from standard Work Manager provided by Application Servers.
    - Dispatch job execution to Message Driven Bean for entering the JEE container (synchronous invocation, no JMS).
    - Use default Job Execution Command from Activiti.
* Integrated Test Suite (ITS) showing how to use it.

## Out of Scope
* Dedicated mechanism to execute asynchronous work within application context. (A custom classloader in Activiti could take care of that)
* Dedicated mechanism to deploy web- or enterprise-applications. (You could register the application classloader upon deployment of an application)

Provided by [agito](http://www.agito-it.com).