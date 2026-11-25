"""Actual saved editor LOWER_ONLY battery approach excavation."""
from pathlib import Path
import author_stair_terrain as author
BASE=Path(__file__).resolve().parents[1]/'development/levels/rebuild'
author.OUT=BASE/'stair-connection-pass/terrain';author.PLANS={'WLD-17':['route-03']}
author.source=lambda site:(BASE/'stair-landing-pass'/site.lower(),'supported-stairs')
if __name__=='__main__':author.main(preview_root=BASE/'captures/stair-connection-batch')
