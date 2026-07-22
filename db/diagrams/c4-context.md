# ReconX Context Diagram

```mermaid
graph LR

Trader[Trader]
Operations[Operations Analyst]
Support[Support Engineer]
Compliance[Compliance Officer]

ReconX[ReconX Platform]

OMS[Order Management System]
SFTP[SFTP Service]
Bloomberg[Bloomberg]
Email[Email Service]
SSO[Corporate SSO]
Grafana[Grafana]

Trader -->|HTTPS| ReconX
Operations -->|HTTPS| ReconX
Support -->|HTTPS| ReconX
Compliance -->|HTTPS| ReconX

ReconX -->|Trade Data| OMS
ReconX -->|Market Data| Bloomberg
ReconX -->|File Exchange| SFTP
ReconX -->|Notifications| Email
ReconX -->|Authentication| SSO
Grafana -->|Monitoring| ReconX
```