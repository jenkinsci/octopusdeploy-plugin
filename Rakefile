
task :default => :bundle

task :build do 
  raise 'Build failed' unless system 'mvn compile'
end

task :bundle do
  raise 'Packaging failed!' unless system 'mvn package'
end

task :demo do
  raise 'Starting demo failed!' unless system 'mvn hpi:run'
end