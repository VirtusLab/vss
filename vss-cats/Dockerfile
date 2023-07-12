FROM hseeberger/scala-sbt

WORKDIR /..

# Copy your source code into the container
COPY . ..

# Compile your Scala application
RUN sbt compile

# Define the command to start your application
CMD ["sbt", "vss_cats/run"]
