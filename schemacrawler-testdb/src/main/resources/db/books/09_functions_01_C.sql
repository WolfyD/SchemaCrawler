-- Functions
-- Oracle syntax
CREATE OR REPLACE FUNCTION CustomAdd(One IN INTEGER) 
RETURN INTEGER
AS 
BEGIN
  RETURN One + 1;
END;
@

CREATE OR REPLACE FUNCTION CustomAdd(One IN INTEGER, Two IN INTEGER) 
RETURN INTEGER
AS 
BEGIN
  RETURN One + Two;
END;
@
