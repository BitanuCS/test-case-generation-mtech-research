# Source this before working: `source env.sh`
# Tools live in a path WITHOUT spaces (Defects4J breaks on spaces).
export TOOLS=/Users/bitu/mtech-tools
export JAVA_HOME=/opt/homebrew/opt/openjdk@11
export PATH="$JAVA_HOME/bin:$TOOLS/defects4j/framework/bin:$PATH"
export PERL5LIB="$TOOLS/perl5/lib/perl5${PERL5LIB:+:$PERL5LIB}"
export TZ=America/Los_Angeles   # Defects4J requires this timezone
