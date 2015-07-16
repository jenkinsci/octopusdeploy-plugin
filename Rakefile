require 'rake/clean'

CLEAN.include 'work', 'target'

task :default => :bundle



task :build do 
  raise 'Build failed' unless system 'mvn enforcer:enforce compile'
end

task :bundle do
  raise 'Packaging failed!' unless system 'mvn enforcer:enforce package'
end

task :demo do
  raise 'Starting demo failed!' unless system 'mvn enforcer:enforce hpi:run'
end