# Prime Number Stream Service

Get all prime numbers up until and including a target number

## Assumptions

- The instructions specify the broad definition `number`, I've assumed that this means `integers`. The service does therefore only handle these, not `floats`, `hex`, `binary`, etc
- The requirement is for the service to handle *up to a given \<number\>*, but the example given (`/prime/17`) returns up to **and including** 17. I've assumed that the service should therefore return *up to and including \<number\>*
