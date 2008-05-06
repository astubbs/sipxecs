#!@PERL@ 

if (m/^INSERT\s+INTO\s+(\w+)\s+\(([^)]+)\)\s+VALUES\s+\((.+?)\);$/i )
{
    my ($table, $col_str, $val_str) = ($1, $2, $3);

    my @columns = split /,\s+/, $col_str;
    my $numColumns = $#columns + 1;
    
    my @values = &getValues($val_str);

    for( $i=0; $i < $numColumns; $i++ )
    {
        if ($columns[$i] =~ /(pintoken|sip_password|secret|shared_secret)/)
        {
            $values[$i] = "'ELIDED'";
        }
    }

    $_ = "INSERT INTO $table ($col_str) VALUES (".join(", ", @values).");\n";
}

sub getValues
{
    my $allValues = shift;
    my @values;

    while ( $allValues )
    {
        $allValues =~ s/^\s+//;
        if ($allValues =~ m/(\w+|'(?:[^']|'')*'),/)
        {
            push @values, $1;
            $allValues = $'; #'
        }
        else
        {
            push @values, $allValues;
            $allValues = '';
        }
    }
    return @values;
}
