# rstuf_maven_demo
This is a demo for RSTUF integration with a Maven repository


## Install Apache HTTP

0. `brew install httpd`
1. Add `127.0.0.1       my.maven.repo` to `/etc/hosts`
2. `brew services start httpd`
3. Add the following to `/usr/local/etc/httpd/httpd.conf`:

```xml
# reposilite is listening on 127.0.0.1:8081
<IfModule mod_rewrite.c>
    RewriteEngine On

    RewriteCond %{HTTP:Upgrade} =websocket [NC]
    RewriteRule ^/api/(.*) ws://127.0.0.1:8081/api/$1 [P,L]
<IfModule mod_rewrite.c>

ProxyPass / http://127.0.0.1:8081/
ProxyPassReverse / http://127.0.0.1:8081/
```
4. `brew services reload httpd`


## Run reposilite

```
docker run -it -v reposilite-data:/app/data -p 80:8080 dzikoysk/reposilite:3.5.6
```

Generate token with 
```
token-generate --secret=DemoPWD03 mavendemo m
```

Open http://my.maven.repo to check it's working



## Setup RSTUF

0. Configure storage ports to not conflict with reposilite:
    - Edit `repository-service-tuf-api/docker-compose.yml`: change port `8080` to `8083` and port `80` to `83` (for every occurence)
    - Run RSTUF (from `repository-service-tuf-api` - check the instructions there)
1. Open the RSTUF API and submit `POST /api/v1/bootstrap/`

## Compile the java packages

0. Setup the reposilite authentication:
 - Add the following to the `<servers></servers>` section within the maven `settings.xml`:
```
    <server>
      <!-- Id has to match the id provided in pom.xml -->
      <id>reposilite-repository</id>
      <username>mavendemo</username>
      <password>DemoPWD03</password>
    </server>
```
> Settings location should be something like `/usr/local/Cellar/maven/<your-version>/libexec/conf/settings.xml`
1. `cd mvn-demo-package/demopackage-app`
2. Run `mvn deploy`

## Add the repository and artifacts to RSTUF

0. Open the RSTUF API and go to `POST /api/v1/artifacts/`
1. Use the following payload and submit:
```
{
  "artifacts": [
    {
      "info": {
        "hashes": {
          "sha256": "cdd413f1238dad5664a8f7e273b94c5d33bd4f0d08e839946356949a22b5f580"
        },
        "length": 3677
      },
      "path": "demopackage-app/1.0/demopackage-app-1.0.jar"
    },
    {
      "info": {
        "hashes": {
          "sha256": "d8f59a8bb7a89189fec1ceca60730a8be68c4bd242808236d4b12f9c1d78c25b"
        },
        "length": 3488
      },
      "path": "demopackage-app/1.0/demopackage-app-1.0.pom"
    },
    {
      "info": {
        "hashes": {
          "sha256": "b6331ef9b3d1af9f83c8ae00743a75d8c15bbe961c939057778280d6b3b057f4"
        },
        "length": 192
      },
      "path": "demopackage-app/1.0/_remote.repositories"
    }
  ]
}
```

> You can verify the hashes with `shasum -a 256 <path_to_jar>`
  and length with `wc -c <path_to_jar>` prior submitting the payload

3. Configure the repository:
 - Install [tufie](https://github.com/kairoaraujo/tufie)
 - Add the repository with
```
tufie repository add --default --artifact-url http://my.maven.repo/releases/com/demopackage/app --metadata-url http://127.0.0.1:8083 --root <path-to-your-root.json> --name mymaven
```

## Setup the commands
Add the following to your `.bash_profile` and run `source ~/.bash_profile`:

``` bash
mvn-tuf-package() {
    tufie download demopackage-app/1.0/demopackage-app-1.0.jar
    (( $? == 0 )) && mvn package
}

mvn-tuf-package-v-bump() {
    tufie download demopackage-app/1.1/demopackage-app-1.1.jar
    (( $? == 0 )) && mvn package
}
```

## Perform the demo


0. `export MVNDEMOPATH=<path to rstuf_maven_demo>`
1. `cd $MVNDEMOPATH/mvn-demo-package/demopackage-app` and run `mvn deploy` for demo purposes
2. `cd $MVNDEMOPATH/demo-app` and compile with `mvn-tuf-package`
You should complie successfully.
3. Run the app with

```
mvn exec:java -Dexec.mainClass="com.mvndemo.app.CompanyDataOperatorApp" -Dexec.args="example-data/newEmployees.json $MVNDEMOPATH/data-storage/allEmployees.csv"
```
4. Verify the contents of `allEmployees.csv`

### MITM Attack
5. Perform the MITM attack
 - Open http://my.maven.repo/#/releases/com/demopackage/app/demopackage-app and delete the 1.0 release
 - `cd $MVNDEMOPATH/malicious-mvn-demo-package/maliciousdemopackage-app` and run `mvn deploy`
6. `cd $MVNDEMOPATH/demo-app` and compile with `mvn-tuf-package`
Downloading artifact should fail and compiling with `mvn package` should never happen.
7. Run the app and check that the contents of `allEmployees.csv` are as expected
8. Compile with `mvn package` and check that the contents of `allEmployees.csv` contain "John The Attacker"

### Bump to malicious version

9. Restore the non-malicious package:
 - Open http://my.maven.repo/#/releases/com/demopackage/app/demopackage-app and delete the 1.0 release
 - `cd $MVNDEMOPATH/mvn-demo-package/demopackage-app` and run `mvn deploy`
10. If you prefer, repeat 3. and 4.
11. Open the `pom.xml` in `$MVNDEMOPATH/mvn-demo-package/demopackage-app` and bump the version to 1.1
12. `cd $MVNDEMOPATH/mvn-demo-package/demopackage-app` and run `mvn deploy`
13. Open http://my.maven.repo/#/releases/com/demopackage/app/demopackage-app to check that a new version is released
14. Open the `pom.xml` in `$MVNDEMOPATH/demo-app` and update the `demopackage-app` dependency version to `1.1`
15. `cd $MVNDEMOPATH/demo-app` and compile with `mvn-tuf-package-v-bump`
Downloading artifact should fail and compiling with `mvn package` should never happen.
16. Run the app and check that the contents of `allEmployees.csv` are as expected
17. Compile with `mvn package` and check that the contents of `allEmployees.csv` contain "John The Attacker"

