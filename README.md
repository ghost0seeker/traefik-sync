## Requirements
1. Connect to a docker socket proxy
2. GET containers and their labels
3. Build a yaml file provider config
4. PUT config inside traefik dynamic path
5. Verify config
	1. if this.didVerify to true else if true, skip to 7. 
	2. Wait for traefik to watch
	3. GET routers and services from traefik api
	4. If routers not updated, restart traefik
	5. Set this.didVerify to true
	6. return to 5. Verify Config
	7. GET routers and services from traefik api
	8. If routers not added, send POST to ntfy topic