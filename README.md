Sample API: URL Shortener
=========================

Demonstrates the enforcement of security policies using Monarch API Manager.  It is meant to be a simply corporate version of [Bitly](https://bitly.com "Bitly homepage"), where only authenticated users can shorten URLs.  A security standard like OAuth or JWT could be used to pass principal information to the API.

The API has 4 operations:

- **Shorten**: Shortens any URL to 6 character token/slug for expansion with an option for specify the value of the slug.  Specifying the slug requires the user to be in the marketing department.  The security check is applied inside the method because the slug parameter is optional.
- **Assign**: An alternative way to shorten a URL using a specific slug value.  The security is applied by using the `@Authorize` annotation with the `claims` attribute.
- **Expand**: Expands the token/slug back to the long URL with an optional flag to increase the visit count by 1.
- **My URLs**: Returns the list of shortened URLs for the current user.

The app assumes that the following Java system properties are set in your app server:

- `api.conf.dir` - The API configuration directory (shared)
- `api.logs.dir` - The API logs directory (shared)


Quickstart
----------

Download and extract [Tomcat 8](http://tomcat.apache.org/download-80.cgi "Tomcat 8 download") and create ${TOMCAT_HOME}/bin/setenv.sh (or .bat for Windows) with the following.

```
export CATALINA_OPTS="$CATALINA_OPTS -Dapi.conf.dir=$CATALINA_BASE/conf -Dapi.logs.dir=$CATALINA_BASE/logs"
export ENC_PWD="1qK6CHCkyhpzJHJuNhgVFzpc"
```

Then add the following user in ${TOMCAT_HOME}/conf/tomcat-users.xml.  This will allow you to deploy the app to Tomcat from Maven.

```
<user name="admin" password="admin" roles="standard,manager-script" />
```

For your convenience, the files in the /conf directory of this project can be copied to the ${TOMCAT_HOME}/conf directory.  They are configured for a simple Monarch setup (Everything running on localhost, Standalone, no SSL, no MongoDB authentication).

To build and deploy, run `mvn clean tomcat:redeploy`

Now, you will need to create the following in the Monarch admin console:

- **Environment**
	- Name = demo
	- System Database = demo
	- Analytics Database = demo
  
- **Access**
	- **Provider**
		- Name = demo
		- API Key = 6kzFoQJedEz5e3OOzaVeKfQ3
		- Shared Secret = LoXOm37LjJ8jd4SjTuQ7O13c
		- Service Permissions:
			- Authenticate API requests
		- Authenticators:
			- Hawk V1
				- SHA-256
				- Require payload validation checked
  - **Principal**
		- Profile = (New) demo
		- Then (New Principal)
			- Name = demo
			- Claims
				- Type = group
				- Values = marketing
- **APIs**
	- **Message**
		- Key = urls
		- Display Order = 1
		- Locales:
			- default
				- Format = Plain Text
				- Content = "Create of shortened URLs."
	- **Permission**
		- Name = urls
		- Description = "Create shortened URLs."
		- Type = Action
		- Scope = Client and User
		- Message = urls
	- **Plan**
		- Name = Free
		- Price = 0
		- Currency = USD
		- Per Minute = 10
		- Per Hour = 100
		- Per Month = 10000
	- **Service**
    	- Name = urlShortener
    	- Note: Access Control settings (URI Prefix, Version, Hostnames, Operations) are not required unless you enable delegated authorization in the `serviceContainer` bean in api-context.xml.

- **Partners**
	- **Application**
		- Application Name = URL Shortener Demo
		- Plan = Free
		- Application Description = Shortens URLs so they can be more easily shared with colleagues.
		- Application Website URL = http://www.company.com
		- Company Name = Company Name
		- Company Website URL = http://www.company.com
		- Callback URLs = http://localhost:8080
	- **Client** (Create under new application)
		- Enabled checked
		- Label = demo
		- API Key = Sl2SkvFjP5h5Yc7XrIt7fTVV
		- Shared Secret = vg6mUREf5MfBFOSnH7XsBxsI
		- Application Permissions = urls
		- Authorization schemes:
			- (New) oauth2-password
				- Enabled
				- Expiration = 1:00:00
				- Life Span = Session
				- User Permissions: Manage globally, "urls" permission checked
		- Authenticators:
			- API Key / Bearer Token (Simple)
				- Checked: Require API Key with Token for verification
		- Claims Sources:
			- (New) Internal
				- Profile = demo