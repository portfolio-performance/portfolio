# Shared helpers for p2 composite repository management.
# Sourced by publish-beta.sh, promote-to-production.sh, prune-releases.sh.

write_composite() {
    local dir=$1
    local version=$2
    local timestamp=$3

    if [ ! -d "$dir" ]; then
        echo "write_composite: directory does not exist: $dir" >&2
        return 1
    fi

    local content_tmp="$dir/compositeContent.xml.tmp"
    local artifacts_tmp="$dir/compositeArtifacts.xml.tmp"

    cat >"$content_tmp" <<EOF
<?xml version='1.0' encoding='UTF-8'?>
<?compositeMetadataRepository version='1.0.0'?>
<repository name='Portfolio Performance' type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository' version='1.0.0'>
  <properties size='1'>
    <property name='p2.timestamp' value='${timestamp}'/>
  </properties>
  <children size='1'>
    <child location='../releases/${version}'/>
  </children>
</repository>
EOF

    cat >"$artifacts_tmp" <<EOF
<?xml version='1.0' encoding='UTF-8'?>
<?compositeArtifactRepository version='1.0.0'?>
<repository name='Portfolio Performance' type='org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository' version='1.0.0'>
  <properties size='1'>
    <property name='p2.timestamp' value='${timestamp}'/>
  </properties>
  <children size='1'>
    <child location='../releases/${version}'/>
  </children>
</repository>
EOF

    mv "$content_tmp" "$dir/compositeContent.xml"
    mv "$artifacts_tmp" "$dir/compositeArtifacts.xml"

    cat >"$dir/p2.index" <<'EOF'
version=1
metadata.repository.factory.order=compositeContent.xml,!
artifact.repository.factory.order=compositeArtifacts.xml,!
EOF
}

current_version() {
    local file=$1

    if [ ! -f "$file" ]; then
        return 0
    fi

    grep -oE "child location='\.\./releases/[^']+'" "$file" \
        | sed -E "s|child location='\.\./releases/([^']+)'|\1|" \
        | head -n 1
}
